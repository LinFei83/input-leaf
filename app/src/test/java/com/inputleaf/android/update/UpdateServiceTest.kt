package com.inputleaf.android.update

import android.content.pm.PackageInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateServiceTest {

    @Test
    fun isNewerVersion_detectsHigherPatch() {
        assertThat(UpdateService.isNewerVersion("1.4.2", "1.4.1")).isTrue()
        assertThat(UpdateService.isNewerVersion("v1.4.2", "1.4.1")).isTrue()
        assertThat(UpdateService.isNewerVersion("1.4.2", "v1.4.1")).isTrue()
    }

    @Test
    fun isNewerVersion_detectsHigherMinorAndMajor() {
        assertThat(UpdateService.isNewerVersion("1.5.0", "1.4.9")).isTrue()
        assertThat(UpdateService.isNewerVersion("2.0.0", "1.9.9")).isTrue()
    }

    @Test
    fun isNewerVersion_returnsFalseWhenEqualOrLower() {
        assertThat(UpdateService.isNewerVersion("1.4.1", "1.4.1")).isFalse()
        assertThat(UpdateService.isNewerVersion("v1.4.1", "1.4.1")).isFalse()
        assertThat(UpdateService.isNewerVersion("1.4.0", "1.4.1")).isFalse()
        assertThat(UpdateService.isNewerVersion("1.3.9", "1.4.1")).isFalse()
        assertThat(UpdateService.isNewerVersion("0.9.9", "1.0.0")).isFalse()
    }

    @Test
    fun resolveInstallSource_detectsFdroidAndDroidify() {
        assertThat(resolveInstallSource("org.fdroid.fdroid")).isEqualTo(InstallSource.FDROID)
        assertThat(resolveInstallSource("com.aurora.store.droidify")).isEqualTo(InstallSource.FDROID)
    }

    @Test
    fun resolveInstallSource_detectsPlayStore() {
        assertThat(resolveInstallSource("com.android.vending")).isEqualTo(InstallSource.PLAY_STORE)
    }

    @Test
    fun resolveInstallSource_defaultsToGithub() {
        assertThat(resolveInstallSource(null)).isEqualTo(InstallSource.GITHUB)
        assertThat(resolveInstallSource("com.github.android")).isEqualTo(InstallSource.GITHUB)
    }

    @Test
    fun versionNameFrom_usesPackageInfoOrFallback() {
        assertThat(versionNameFrom(PackageInfo().apply { versionName = "2.0.0" })).isEqualTo("2.0.0")
        assertThat(versionNameFrom(null)).isEqualTo("1.4.1")
    }

    @Test
    fun versionCodeFrom_usesPackageInfoOrFallback() {
        val packageInfo = PackageInfo().apply {
            @Suppress("DEPRECATION")
            versionCode = 42
        }
        assertThat(versionCodeFrom(packageInfo)).isEqualTo(42L)
        assertThat(versionCodeFrom(null)).isEqualTo(7L)
    }

    @Test
    fun httpErrorResult_formatsMessage() {
        assertThat(httpErrorResult(404).message).isEqualTo("HTTP error 404 from GitHub")
    }

    @Test
    fun parseLatestReleaseResponse_returnsUpdateAvailableForGithub() {
        val body = """
            {
              "tag_name": "v1.5.0",
              "body": "New features",
              "html_url": "https://github.com/anasvhora284/input-leaf/releases/tag/v1.5.0"
            }
        """.trimIndent()

        val result = parseLatestReleaseResponse(body, currentVersion = "1.4.1", isFdroid = false)

        assertThat(result).isInstanceOf(UpdateCheckResult.UpdateAvailable::class.java)
        val update = result as UpdateCheckResult.UpdateAvailable
        assertThat(update.latestVersion).isEqualTo("1.5.0")
        assertThat(update.changelog).isEqualTo("New features")
        assertThat(update.updateUrl).contains("github.com")
        assertThat(update.isFdroid).isFalse()
    }

    @Test
    fun parseLatestReleaseResponse_usesFdroidMarketUrl() {
        val body = """{"tag_name":"v2.0.0","body":"","html_url":""}"""

        val result = parseLatestReleaseResponse(body, currentVersion = "1.0.0", isFdroid = true)

        assertThat(result).isInstanceOf(UpdateCheckResult.UpdateAvailable::class.java)
        val update = result as UpdateCheckResult.UpdateAvailable
        assertThat(update.updateUrl).isEqualTo(UpdateService.FDROID_MARKET_URI)
        assertThat(update.isFdroid).isTrue()
    }

    @Test
    fun parseLatestReleaseResponse_returnsUpToDateWhenNotNewer() {
        val body = """{"tag_name":"v1.4.1","body":"Same version","html_url":""}"""

        val result = parseLatestReleaseResponse(body, currentVersion = "1.4.1", isFdroid = false)

        assertThat(result).isEqualTo(UpdateCheckResult.UpToDate("1.4.1"))
    }

    @Test
    fun checkUpdate_returnsHttpError() = runTest {
        val connection = TestHttpURLConnection(URL("http://example.com"), responseCode = 500)

        val result = UpdateService.checkUpdate(
            currentVersion = "1.4.1",
            isFdroid = false,
            openConnection = { connection },
        )

        assertThat(result).isEqualTo(UpdateCheckResult.Error("HTTP error 500 from GitHub"))
        assertThat(connection.disconnectCalled).isTrue()
    }

    @Test
    fun checkUpdate_returnsParsedRelease() = runTest {
        val body = """{"tag_name":"v9.9.9","body":"Big release","html_url":"https://example.com"}"""
        val connection = TestHttpURLConnection(
            url = URL("http://example.com"),
            responseBody = body,
        )

        val result = UpdateService.checkUpdate(
            currentVersion = "1.0.0",
            isFdroid = false,
            openConnection = { connection },
        )

        assertThat(result).isInstanceOf(UpdateCheckResult.UpdateAvailable::class.java)
        assertThat((result as UpdateCheckResult.UpdateAvailable).latestVersion).isEqualTo("9.9.9")
        assertThat(connection.disconnectCalled).isTrue()
    }

    @Test
    fun checkUpdate_returnsErrorWhenConnectionThrows() = runTest {
        val result = UpdateService.checkUpdate(
            currentVersion = "1.0.0",
            isFdroid = false,
            openConnection = { throw IllegalStateException("network down") },
        )

        assertThat(result).isEqualTo(UpdateCheckResult.Error("network down"))
    }

    @Test
    fun changelogProvider_returnsValidHighlights() {
        val changelog = ChangelogProvider.getChangelog("1.4.1")
        assertThat(changelog.versionName).isEqualTo("1.4.1")
        assertThat(changelog.highlights).isNotEmpty()
    }

    @Test
    fun changelogProvider_fallsBackToLatestKnownRelease() {
        val changelog = ChangelogProvider.getChangelog("9.9.9")
        assertThat(changelog.versionName).isEqualTo(ChangelogProvider.RELEASES.first().versionName)
    }

    @Test
    fun changelogProvider_stripsVersionPrefix() {
        val changelog = ChangelogProvider.getChangelog("v1.4.0")
        assertThat(changelog.versionName).isEqualTo("1.4.0")
    }

    @Test
    fun defaultConnectionOpener_configuresGitHubRequest() {
        val connection = UpdateService.defaultConnectionOpener(URL("http://example.com"))

        assertThat(connection.requestMethod).isEqualTo("GET")
        assertThat(connection.getRequestProperty("Accept")).isEqualTo("application/vnd.github.v3+json")
        assertThat(connection.getRequestProperty("User-Agent")).isEqualTo("InputLeaf-Android")
    }

    private class TestHttpURLConnection(
        url: URL,
        private val responseCode: Int = HTTP_OK,
        private val responseBody: String = "",
    ) : HttpURLConnection(url) {
        var disconnectCalled = false

        override fun disconnect() {
            disconnectCalled = true
        }

        override fun connect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getRequestMethod(): String = "GET"

        override fun getResponseCode(): Int = responseCode

        override fun getResponseMessage(): String = "OK"

        override fun getInputStream() = ByteArrayInputStream(responseBody.toByteArray())

        override fun getErrorStream() = null

        override fun setRequestMethod(method: String) = Unit

        override fun getInstanceFollowRedirects(): Boolean = false

        override fun setInstanceFollowRedirects(followRedirects: Boolean) = Unit

        override fun getConnectTimeout(): Int = 0

        override fun setConnectTimeout(timeout: Int) = Unit

        override fun getReadTimeout(): Int = 0

        override fun setReadTimeout(timeout: Int) = Unit
    }
}
