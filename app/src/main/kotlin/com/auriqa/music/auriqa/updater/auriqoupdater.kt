

package com.auriqo.music.echomusic.updater


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.auriqo.music.BuildConfig
import com.auriqo.music.R
import coil3.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.auriqo.music.echomusic.updater.downloadmanager.UpdateDownloadWorker
import com.auriqo.music.echomusic.updater.downloadmanager.DownloadNotificationManager
import com.auriqo.music.echomusic.UpdateNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern
import com.auriqo.music.ui.component.ChangelogItem
import com.auriqo.music.ui.component.leadingItemShape
import com.auriqo.music.ui.component.middleItemShape
import com.auriqo.music.ui.component.endItemShape
import com.auriqo.music.ui.component.detachedItemShape
import com.auriqo.music.ui.component.parseMarkdown
import com.auriqo.music.ui.component.endItemShape
import com.auriqo.music.ui.component.detachedItemShape
import com.auriqo.music.ui.component.AnimatedActionButton
import com.auriqo.music.ui.component.ExpressiveIconButton
import com.auriqo.music.ui.component.ErrorSnackbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.style.TextDecoration

data class ChangelogSection(val title: String, val items: List<String>)

data class UpdateAsset(
    val name: String,
    val url: String,
    val sizeBytes: Long,
    val digest: String?,
)

data class AuriqoUpdate(
    val tag: String,
    val changelog: List<ChangelogSection>,
    val size: String,
    val releaseDate: String,
    val description: String?,
    val imageUrl: String?,
    val apkUrl: String,
    val assetName: String,
    val sha256: String?,
)

sealed class AuriqoUpdateStatus {
    object Idle : AuriqoUpdateStatus()
    object Checking : AuriqoUpdateStatus()
    data class Available(
        val version: String,
        val changelog: List<ChangelogSection>,
        val size: String,
        val releaseDate: String,
        val description: String?,
        val imageUrl: String?,
        val apkUrl: String?,
        val assetName: String,
        val sha256: String?
    ) : AuriqoUpdateStatus()

    data class NoUpdate(val version: String) : AuriqoUpdateStatus()
    data class Error(val message: String) : AuriqoUpdateStatus()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<AuriqoUpdateStatus>(AuriqoUpdateStatus.NoUpdate(BuildConfig.VERSION_NAME)) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val autoUpdateCheckEnabled = getAutoUpdateCheckSetting(context)

    LaunchedEffect(Unit) {
        DownloadNotificationManager.initialize(context)
    }

    
    LaunchedEffect(Unit) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("update_download")
            .observeForever { workInfos ->
                val workInfo = workInfos?.firstOrNull() ?: return@observeForever

                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        isDownloading = true
                        downloadProgress = workInfo.progress.getFloat("progress", 0f)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        isDownloading = false
                        isDownloadComplete = true
                        val filePath = workInfo.outputData.getString("file_path")
                        if (filePath != null) {
                            downloadedFile = File(filePath)
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        isDownloading = false
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.download_failed))
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        isDownloading = false
                        downloadProgress = 0f
                    }
                    else -> {}
                }
            }
    }

    
    LaunchedEffect(isDownloadComplete, downloadedFile) {
        if (isDownloadComplete && downloadedFile != null) {
            if (!downloadedFile!!.exists()) {
                isDownloadComplete = false
                downloadedFile = null
                downloadProgress = 0f
            }
        }
    }

    fun triggerUpdateCheck() {
        status = AuriqoUpdateStatus.Checking
        scope.launch {
            
            delay(1000L)
            checkForUpdate(
                context = context,
                onSuccess = { update ->
                    saveLastCheckedTime(context, LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")))
                    saveUpdateCheckResult(context, update, notify = false)
                    status = update?.let {
                        AuriqoUpdateStatus.Available(
                            version = it.tag,
                            changelog = it.changelog,
                            size = it.size,
                            releaseDate = it.releaseDate,
                            description = it.description,
                            imageUrl = it.imageUrl,
                            apkUrl = it.apkUrl,
                            assetName = it.assetName,
                            sha256 = it.sha256,
                        )
                    } ?: AuriqoUpdateStatus.NoUpdate(BuildConfig.VERSION_NAME)
                },
                onError = {
                    status = AuriqoUpdateStatus.Error(context.getString(R.string.cant_check_updates))
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (autoUpdateCheckEnabled) {
            triggerUpdateCheck()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    val titleText = if (status is AuriqoUpdateStatus.Available) {
                        buildAnnotatedString {
                            append(stringResource(R.string.new_update) + " ")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append((status as AuriqoUpdateStatus.Available).version)
                            }
                        }
                    } else {
                        AnnotatedString(stringResource(R.string.settings_check_updates_title))
                    }
                    Text(text = titleText, maxLines = 1)
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                        ExpressiveIconButton(
                            onClick = { navController.navigateUp() },
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.cancel),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val currentStatus = status) {
                        is AuriqoUpdateStatus.Idle, is AuriqoUpdateStatus.Checking, is AuriqoUpdateStatus.NoUpdate, is AuriqoUpdateStatus.Error -> {
                            AnimatedActionButton(
                                text = stringResource(R.string.check_for_update),
                                onClick = { triggerUpdateCheck() },
                                enabled = currentStatus !is AuriqoUpdateStatus.Checking && !isDownloading,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is AuriqoUpdateStatus.Available -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedActionButton(
                                    text = stringResource(R.string.later),
                                    onClick = { navController.navigateUp() },
                                    modifier = Modifier.weight(1f),
                                    isOutlined = true,
                                    enabled = !isDownloading
                                )
                                AnimatedActionButton(
                                    text = if (isDownloading) "${(downloadProgress * 100).toInt()}%" else if (isDownloadComplete) stringResource(R.string.install) else stringResource(R.string.update_available),
                                    onClick = {
                                        if (isDownloadComplete) {
                                            val file = downloadedFile
                                            if (file == null || !file.exists()) {
                                                isDownloadComplete = false
                                                downloadedFile = null
                                                downloadProgress = 0f
                                                return@AnimatedActionButton
                                            }

                                            if (!isValidUpdateApk(context, file)) {
                                                file.delete()
                                                isDownloadComplete = false
                                                downloadedFile = null
                                                downloadProgress = 0f
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.invalid_update_package))
                                                }
                                                return@AnimatedActionButton
                                            }

                                            if (!canRequestUpdateInstall(context)) {
                                                ContextCompat.startActivity(
                                                    context,
                                                    createUnknownSourcesSettingsIntent(context),
                                                    null,
                                                )
                                                return@AnimatedActionButton
                                            }

                                            ContextCompat.startActivity(
                                                context,
                                                createUpdateInstallIntent(context, file),
                                                null,
                                            )
                                        } else {
                                            val urlToDownload = currentStatus.apkUrl

                                            val constraints = Constraints.Builder()
                                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                                .build()

                                            val downloadRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                                                .setInputData(
                                                    workDataOf(
                                                        "apk_url" to urlToDownload,
                                                        "version" to currentStatus.version,
                                                        "file_size" to currentStatus.size,
                                                        "expected_sha256" to currentStatus.sha256.orEmpty(),
                                                        "file_name" to currentStatus.assetName,
                                                    )
                                                )
                                                .setConstraints(constraints)
                                                .setBackoffCriteria(
                                                    BackoffPolicy.EXPONENTIAL,
                                                    10,
                                                    java.util.concurrent.TimeUnit.SECONDS
                                                )
                                                .addTag("update_download")
                                                .build()
                                            WorkManager.getInstance(context).enqueueUniqueWork("update_download", ExistingWorkPolicy.REPLACE, downloadRequest)
                                            isDownloading = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isDownloading || isDownloadComplete
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { ErrorSnackbar(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .widthIn(max = 700.dp)
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    val contentModifier = if (status is AuriqoUpdateStatus.Available) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.fillParentMaxSize()
                    }

                    Box(
                        modifier = contentModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        when (val currentStatus = status) {
                            is AuriqoUpdateStatus.Checking -> {
                                androidx.compose.material3.ContainedLoadingIndicator(
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                            is AuriqoUpdateStatus.NoUpdate -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.deployed_app_update),
                                        contentDescription = null,
                                        modifier = Modifier.size(120.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = stringResource(R.string.on_latest_version),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.current_version_v, currentStatus.version),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            is AuriqoUpdateStatus.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.error),
                                        contentDescription = null,
                                        modifier = Modifier.size(120.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = currentStatus.message,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            is AuriqoUpdateStatus.Available -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.release_date_v, currentStatus.releaseDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.update_size_v, currentStatus.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    if (!currentStatus.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = currentStatus.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(24.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                    if (!currentStatus.description.isNullOrBlank()) {
                                        val annotatedText = currentStatus.description.parseMarkdown()

                                        ClickableText(
                                            text = annotatedText,
                                            onClick = { offset ->
                                                annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                                    ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                                                }
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 20.sp
                                            ),
                                            modifier = Modifier.padding(bottom = 24.dp)
                                        )
                                    }
                                    
                                    currentStatus.changelog.forEach { section ->
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                        )
                                        section.items.forEachIndexed { index, item ->
                                            val shape = when {
                                                section.items.size == 1 -> detachedItemShape()
                                                index == 0 -> leadingItemShape()
                                                index == section.items.size - 1 -> endItemShape()
                                                else -> middleItemShape()
                                            }
                                            ChangelogItem(text = item, shape = shape)
                                            if (index != section.items.size - 1) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                    
                                    if (currentStatus.changelog.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    if (isDownloading) {
                                        if (downloadProgress > 0f) {
                                            androidx.compose.material3.LinearProgressIndicator(
                                                progress = downloadProgress,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                        } else {
                                            androidx.compose.material3.LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}



const val PREFS_NAME = "settings"
const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
const val KEY_LAST_CHECKED_TIME = "last_checked_time"
const val KEY_BETA_UPDATES = "beta_updates"
const val KEY_UPDATE_AVAILABLE = "update_available"

fun getUpdateAvailableState(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_AVAILABLE, false)
}

fun saveUpdateAvailableState(context: Context, available: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_AVAILABLE, available).apply()
}

fun getAutoUpdateCheckSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)
}

fun saveAutoUpdateCheckSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, enabled).apply()
}

const val KEY_UPDATE_NOTIFICATIONS = "update_notifications"

fun getUpdateNotificationsSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_NOTIFICATIONS, true)
}

fun saveUpdateNotificationsSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_NOTIFICATIONS, enabled).apply()
}

fun saveLastCheckedTime(context: Context, timestamp: String) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(KEY_LAST_CHECKED_TIME, timestamp).apply()
}

fun getLastCheckedTime(context: Context): String {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getString(KEY_LAST_CHECKED_TIME, "") ?: ""
}

fun getBetaUpdatesSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_BETA_UPDATES, false)
}

fun saveBetaUpdatesSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_BETA_UPDATES, enabled).apply()
}

private fun formatGitHubDate(githubDate: String): String = try {
    val githubFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
    val dateTime = LocalDateTime.parse(githubDate, githubFormatter)
    dateTime.format(displayFormatter)
} catch (e: Exception) {
    githubDate
}


private data class ParsedVersion(
    val parts: List<Int>,
    val prerelease: Boolean,
)

private fun parseVersion(raw: String): ParsedVersion {
    val normalized = raw.trim().removePrefix("v").removePrefix("b")
    val core = normalized.substringBefore("-").substringBefore("+")
    val parts = core.split(".").mapNotNull { it.toIntOrNull() }
    return ParsedVersion(
        parts = parts.ifEmpty { listOf(0) },
        prerelease = normalized != core,
    )
}

fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    val latest = parseVersion(latestVersion)
    val current = parseVersion(currentVersion)

    for (index in 0 until maxOf(latest.parts.size, current.parts.size)) {
        val latestPart = latest.parts.getOrElse(index) { 0 }
        val currentPart = current.parts.getOrElse(index) { 0 }
        if (latestPart != currentPart) return latestPart > currentPart
    }

    return current.prerelease && !latest.prerelease
}

internal fun selectUpdateAsset(
    assets: List<UpdateAsset>,
    variant: String,
    debug: Boolean,
): UpdateAsset? {
    val variantToken = variant.lowercase(Locale.US)
    val buildToken = if (debug) "debug" else "release"
    val preferredName = "app-universal-" + variantToken + "-" + buildToken + ".apk"

    val candidates = assets.filter { asset ->
        val name = asset.name.lowercase(Locale.US)
        name.endsWith(".apk") &&
            name.contains(variantToken) &&
            if (debug) name.contains("debug") else !name.contains("debug")
    }

    return candidates.firstOrNull { it.name.equals(preferredName, ignoreCase = true) }
        ?: candidates.minByOrNull { it.name.length }
}

private fun normalizeDigest(rawDigest: String?): String? {
    val digest = rawDigest
        ?.substringAfter(":", rawDigest)
        ?.trim()
        ?.lowercase(Locale.US)
        ?: return null
    return digest.takeIf { digest.length == 64 && digest.all { it in "0123456789abcdef" } }
}

private fun readRemoteText(urlString: String): String {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 15_000
    connection.requestMethod = "GET"
    connection.setRequestProperty("Accept", "application/vnd.github+json")
    connection.setRequestProperty("User-Agent", "Auriqo/" + BuildConfig.VERSION_NAME)

    return try {
        if (connection.responseCode !in 200..299) {
            throw IOException("HTTP " + connection.responseCode)
        }
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

suspend fun fetchLatestUpdate(context: Context): AuriqoUpdate? = withContext(Dispatchers.IO) {
    val releases = JSONArray(
        readRemoteText("https://api.github.com/repos/Auriqo/Auriqo/releases?per_page=20")
    )
    val variant = if (BuildConfig.CAST_AVAILABLE) "gms" else "foss"

    for (index in 0 until releases.length()) {
        val release = releases.getJSONObject(index)
        if (release.optBoolean("draft", false)) continue
        if (release.optBoolean("prerelease", false) && !getBetaUpdatesSetting(context)) continue

        val tag = release.optString("tag_name").takeIf { it.isNotBlank() } ?: continue
        if (!isNewerVersion(tag, BuildConfig.VERSION_NAME)) continue

        val assets = buildList {
            val releaseAssets = release.optJSONArray("assets") ?: return@buildList
            for (assetIndex in 0 until releaseAssets.length()) {
                val asset = releaseAssets.getJSONObject(assetIndex)
                add(
                    UpdateAsset(
                        name = asset.optString("name"),
                        url = asset.optString("browser_download_url"),
                        sizeBytes = asset.optLong("size", 0L),
                        digest = asset.optString("digest").takeIf { it.isNotBlank() },
                    )
                )
            }
        }
        val selectedAsset = selectUpdateAsset(assets, variant, BuildConfig.DEBUG) ?: continue

        val changelog = mutableListOf<ChangelogSection>()
        var description: String? = null
        var imageUrl: String? = null
        try {
            val changelogData = JSONObject(
                readRemoteText(
                    "https://github.com/Auriqo/Auriqo/releases/download/" +
                        tag + "/changelog.json"
                )
            )
            description = changelogData.optString("description").takeIf { it.isNotBlank() }
            imageUrl = changelogData.optString("image").takeIf { it.isNotBlank() }
            val changelogArray = changelogData.optJSONArray("changelog") ?: JSONArray()
            for (sectionIndex in 0 until changelogArray.length()) {
                val section = changelogArray.getJSONObject(sectionIndex)
                val items = buildList {
                    val itemArray = section.optJSONArray("items") ?: JSONArray()
                    for (itemIndex in 0 until itemArray.length()) {
                        add(itemArray.getString(itemIndex))
                    }
                }
                changelog += ChangelogSection(section.optString("title"), items)
            }
        } catch (_: Exception) {
            var body = release.optString("body", context.getString(R.string.no_changelog_available))
            val imageRegex = Regex("!\\[(.*?)\\]\\((.*?)\\)")
            val match = imageRegex.find(body)
            if (match != null) {
                imageUrl = match.groupValues[2]
                body = body.replace(match.value, "").trim()
            }
            description = body.takeIf { it.isNotBlank() }
        }

        val publishedAt = release.optString("published_at").ifBlank {
            release.optString("created_at")
        }
        val sizeInMb = if (selectedAsset.sizeBytes > 0) {
            String.format(Locale.US, "%.1f", selectedAsset.sizeBytes / (1024.0 * 1024.0))
        } else {
            ""
        }

        return@withContext AuriqoUpdate(
            tag = tag,
            changelog = changelog,
            size = sizeInMb,
            releaseDate = formatGitHubDate(publishedAt),
            description = description,
            imageUrl = imageUrl,
            apkUrl = selectedAsset.url,
            assetName = selectedAsset.name,
            sha256 = normalizeDigest(selectedAsset.digest),
        )
    }

    return@withContext null
}

suspend fun checkForUpdate(
    context: Context,
    onSuccess: (AuriqoUpdate?) -> Unit,
    onError: () -> Unit,
) {
    try {
        val update = fetchLatestUpdate(context)
        withContext(Dispatchers.Main) {
            onSuccess(update)
        }
    } catch (error: Exception) {
        Log.e("UpdateCheck", "Error checking for updates", error)
        withContext(Dispatchers.Main) {
            onError()
        }
    }
}

private const val KEY_LAST_NOTIFIED_UPDATE = "last_notified_update"
private val updateNotificationLock = Any()

fun saveUpdateCheckResult(
    context: Context,
    update: AuriqoUpdate?,
    notify: Boolean,
) {
    saveUpdateAvailableState(context, update != null)
    if (!notify || update == null || !getUpdateNotificationsSetting(context)) return

    synchronized(updateNotificationLock) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_NOTIFIED_UPDATE, null) == update.tag) return
        if (UpdateNotificationHelper.showUpdateNotification(context, update.tag)) {
            prefs.edit().putString(KEY_LAST_NOTIFIED_UPDATE, update.tag).apply()
        }
    }
}
fun String.extractUrls(): List<Pair<IntRange, String>> {
    val urlPattern = Pattern.compile(
        "(?:^|[\\s])((https?://|www\\.|pic\\.)[\\w-]+(\\.[\\w-]+)+([/?].*)?)"
    )
    val matcher = urlPattern.matcher(this)
    val urlList = mutableListOf<Pair<IntRange, String>>()

    while (matcher.find()) {
        val url = matcher.group(1)?.trim() ?: continue
        val range = IntRange(matcher.start(1), matcher.end(1) - 1)
        
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        urlList.add(range to fullUrl)
    }

    return urlList
}
