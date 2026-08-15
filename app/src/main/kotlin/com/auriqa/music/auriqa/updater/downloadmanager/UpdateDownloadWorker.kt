package com.auriqo.music.echomusic.updater.downloadmanager

import android.content.Context
import android.os.Build
import android.os.Environment
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.auriqo.music.R
import com.auriqo.music.echomusic.updater.isValidUpdateApk
import com.auriqo.music.echomusic.updater.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class UpdateDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val apkUrl = inputData.getString("apk_url") ?: return@withContext Result.failure()
        val version = inputData.getString("version") ?: "unknown"
        val fileSize = inputData.getString("file_size") ?: ""
        val expectedSha256 = inputData.getString("expected_sha256").orEmpty()
        val requestedFileName = inputData.getString("file_name").orEmpty()

        if (!apkUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure()
        }

        DownloadNotificationManager.initialize(context)

        try {
            val startingNotification = DownloadNotificationManager.getDownloadStartingNotification(version, fileSize)
            val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    DownloadNotificationManager.NOTIFICATION_ID,
                    startingNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                ForegroundInfo(DownloadNotificationManager.NOTIFICATION_ID, startingNotification)
            }
            setForeground(foregroundInfo)
        } catch (_: Exception) {
            // The foreground notification is best effort; the worker still reports a useful error.
        }

        try {
            val downloadDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "echo_updates",
            )
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                return@withContext Result.failure()
            }

            val urlPath = apkUrl.substringBefore("?").lowercase()
            val isZip = urlPath.contains("nightly.link") || urlPath.endsWith(".zip")
            val fileName = safeFileName(requestedFileName, if (isZip) "auriqa_update.zip" else "auriqa.apk")
            val downloadFile = File(downloadDir, if (isZip) "auriqa_update.zip" else fileName)
            val connection = URL(apkUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/octet-stream")

            try {
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    DownloadNotificationManager.showDownloadFailed(
                        version,
                        context.getString(R.string.server_error, connection.responseCode),
                    )
                    return@withContext if (connection.responseCode >= 500) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }

                val fileLength = connection.contentLengthLong
                connection.inputStream.use { inputStream ->
                    FileOutputStream(downloadFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var totalBytesRead = 0L
                        var lastProgress = -1
                        var lastNotificationTime = 0L

                        while (true) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break

                            if (isStopped) {
                                downloadFile.delete()
                                return@withContext Result.retry()
                            }

                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (fileLength > 0) {
                                val progress = (totalBytesRead.toDouble() / fileLength * 100).toInt()
                                val currentTime = System.currentTimeMillis()
                                if (progress > lastProgress && currentTime - lastNotificationTime >= 1000) {
                                    lastProgress = progress
                                    lastNotificationTime = currentTime
                                    val progressNotification =
                                        DownloadNotificationManager.getDownloadProgressNotification(progress, version)
                                    val notificationManager =
                                        context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                    notificationManager.notify(
                                        DownloadNotificationManager.NOTIFICATION_ID,
                                        progressNotification,
                                    )
                                    setProgress(workDataOf("progress" to progress / 100f))
                                }
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (expectedSha256.isNotBlank() &&
                !sha256(downloadFile).equals(expectedSha256, ignoreCase = true)
            ) {
                downloadFile.delete()
                DownloadNotificationManager.showDownloadFailed(
                    version,
                    context.getString(R.string.checksum_mismatch),
                )
                return@withContext Result.failure()
            }

            val finalFile = if (isZip) {
                val targetApkFile = File(downloadDir, fileName)
                var extracted = false
                try {
                    ZipInputStream(downloadFile.inputStream()).use { zipInputStream ->
                        var entry = zipInputStream.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                                FileOutputStream(targetApkFile).use { outputStream ->
                                    zipInputStream.copyTo(outputStream)
                                }
                                extracted = true
                                break
                            }
                            entry = zipInputStream.nextEntry
                        }
                    }
                } catch (error: Exception) {
                    DownloadNotificationManager.showDownloadFailed(
                        version,
                        error.message ?: context.getString(R.string.download_failed),
                    )
                    return@withContext Result.failure()
                } finally {
                    downloadFile.delete()
                }

                if (!extracted) {
                    DownloadNotificationManager.showDownloadFailed(
                        version,
                        context.getString(R.string.invalid_update_package),
                    )
                    return@withContext Result.failure()
                }
                targetApkFile
            } else {
                downloadFile
            }

            if (!isValidUpdateApk(context, finalFile)) {
                finalFile.delete()
                DownloadNotificationManager.showDownloadFailed(
                    version,
                    context.getString(R.string.invalid_update_package),
                )
                return@withContext Result.failure()
            }

            if (version.startsWith("nightly-r")) {
                val runNumber = version.removePrefix("nightly-r").toIntOrNull()
                if (runNumber != null) {
                    context.getSharedPreferences("update_settings", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("last_installed_nightly_run", runNumber)
                        .apply()
                }
            }

            DownloadNotificationManager.showDownloadComplete(version, finalFile.absolutePath)
            Result.success(workDataOf("file_path" to finalFile.absolutePath))
        } catch (error: IOException) {
            DownloadNotificationManager.showDownloadFailed(
                version,
                error.message ?: context.getString(R.string.download_failed),
            )
            Result.retry()
        } catch (error: Exception) {
            DownloadNotificationManager.showDownloadFailed(
                version,
                error.message ?: context.getString(R.string.download_failed),
            )
            Result.failure()
        }
    }

    private fun safeFileName(requestedName: String, fallback: String): String {
        val candidate = requestedName
            .substringAfterLast('/')
            .map { character ->
                if (character.isLetterOrDigit() || character == '.' || character == '-' || character == '_') {
                    character
                } else {
                    '_'
                }
            }
            .joinToString("")
        return candidate.takeIf { it.endsWith(".apk", ignoreCase = true) } ?: fallback
    }
}
