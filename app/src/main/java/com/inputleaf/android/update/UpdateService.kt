package com.inputleaf.android.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

enum class InstallSource {
    FDROID,
    PLAY_STORE,
    GITHUB
}

sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val changelog: String,
        val updateUrl: String,
        val isFdroid: Boolean
    ) : UpdateCheckResult

    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object UpdateService {

    private const val GITHUB_API_LATEST_RELEASE =
        "https://api.github.com/repos/anasvhora284/input-leaf/releases/latest"
    private const val FDROID_MARKET_URI = "market://details?id=com.inputleaf.android"
    private const val FDROID_WEB_URL = "https://f-droid.org/packages/com.inputleaf.android/"
    private const val GITHUB_RELEASES_WEB_URL = "https://github.com/anasvhora284/input-leaf/releases/latest"

    fun getInstallSource(context: Context): InstallSource {
        val pm = context.packageManager
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(context.packageName)
            }
        } catch (_: Exception) {
            null
        }

        return when {
            installer != null && (installer.contains("fdroid", ignoreCase = true) || installer.contains("droidify", ignoreCase = true)) ->
                InstallSource.FDROID
            installer != null && installer.contains("vending", ignoreCase = true) ->
                InstallSource.PLAY_STORE
            else ->
                InstallSource.GITHUB
        }
    }

    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.4.1"
        } catch (_: Exception) {
            "1.4.1"
        }
    }

    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            7L
        }
    }

    suspend fun checkUpdate(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersion(context)
        val installSource = getInstallSource(context)
        val isFdroid = installSource == InstallSource.FDROID

        try {
            val url = URL(GITHUB_API_LATEST_RELEASE)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "InputLeaf-Android")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("HTTP error $responseCode from GitHub")
            }

            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val json = JSONObject(responseBody)

            val rawTagName = json.optString("tag_name", "").trim()
            val latestVersion = rawTagName.removePrefix("v").removePrefix("V")
            val releaseNotes = json.optString("body", "").trim()
            val githubHtmlUrl = json.optString("html_url", GITHUB_RELEASES_WEB_URL)

            val targetUrl = if (isFdroid) {
                // If the user has an F-Droid client installed, market:// will open it directly
                FDROID_MARKET_URI
            } else {
                githubHtmlUrl.ifEmpty { GITHUB_RELEASES_WEB_URL }
            }

            if (isNewerVersion(latestVersion, currentVersion)) {
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = latestVersion,
                    changelog = releaseNotes,
                    updateUrl = targetUrl,
                    isFdroid = isFdroid
                )
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Failed to check for updates")
        }
    }

    /**
     * Compare two semantic version strings (e.g. "1.4.2" vs "1.4.1").
     * Returns true if candidate > current.
     */
    fun isNewerVersion(candidate: String, current: String): Boolean {
        if (candidate.isBlank() || current.isBlank()) return false
        val cleanCandidate = candidate.removePrefix("v").removePrefix("V").split("-")[0]
        val cleanCurrent = current.removePrefix("v").removePrefix("V").split("-")[0]

        val candidateParts = cleanCandidate.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(candidateParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val candPart = candidateParts.getOrElse(i) { 0 }
            val currPart = currentParts.getOrElse(i) { 0 }
            if (candPart > currPart) return true
            if (candPart < currPart) return false
        }
        return false
    }
}
