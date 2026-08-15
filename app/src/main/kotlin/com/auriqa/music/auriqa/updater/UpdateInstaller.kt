package com.auriqo.music.echomusic.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.auriqo.music.BuildConfig
import java.io.File
import java.security.MessageDigest

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

fun canRequestUpdateInstall(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        context.packageManager.canRequestPackageInstalls()
}

fun createUnknownSourcesSettingsIntent(context: Context): Intent {
    return Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.parse("package:${context.packageName}")
    }
}

fun createUpdateInstallIntent(context: Context, apkFile: File): Intent {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.FileProvider",
        apkFile,
    )
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, APK_MIME_TYPE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

/**
 * Checks that a downloaded APK can update this installation before handing it to Android.
 * The package and signing certificate checks keep a wrong-variant or unrelated APK from
 * reaching the system installer.
 */
fun isValidUpdateApk(context: Context, apkFile: File): Boolean {
    if (!apkFile.isFile || !apkFile.canRead()) return false

    val packageManager = context.packageManager
    val signingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }
    val archiveInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, signingFlags)
        ?: return false

    if (archiveInfo.packageName != context.packageName) return false
    if (PackageInfoCompat.getLongVersionCode(archiveInfo) < BuildConfig.VERSION_CODE.toLong()) return false

    val installedInfo = packageManager.getPackageInfo(context.packageName, signingFlags)
    val archiveCertificates = signingCertificates(archiveInfo, includeHistory = false)
    val installedCertificates = signingCertificates(installedInfo, includeHistory = true)
    return archiveCertificates.isNotEmpty() && archiveCertificates.all { it in installedCertificates }
}

private fun signingCertificates(packageInfo: PackageInfo, includeHistory: Boolean): Set<String> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signingInfo = packageInfo.signingInfo ?: return emptySet()
        if (signingInfo.hasMultipleSigners() || !includeHistory) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
    } else {
        @Suppress("DEPRECATION")
        packageInfo.signatures ?: return emptySet()
    }

    return signatures.map { it.toByteArray().toHexString() }.toSet()
}

private fun ByteArray.toHexString(): String =
    joinToString("") { "%02x".format(it.toInt() and 0xff) }
