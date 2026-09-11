package com.inputleaf.android.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateServiceContextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getCurrentVersion_readsInstalledVersion() {
        assertThat(UpdateService.getCurrentVersion(context)).isNotEmpty()
    }

    @Test
    fun getCurrentVersionCode_readsInstalledVersionCode() {
        assertThat(UpdateService.getCurrentVersionCode(context)).isGreaterThan(0L)
    }

    @Test
    fun getInstallSource_returnsKnownSource() {
        assertThat(UpdateService.getInstallSource(context)).isNotNull()
    }

    @Test
    fun readModernInstallerPackageName_readsInstallSourceOnDevice() {
        readModernInstallerPackageName(context)
    }

    @Test
    fun readModernPackageInfo_readsPackageInfoOnDevice() {
        assertThat(readModernPackageInfo(context).packageName).isEqualTo(context.packageName)
    }

    @Test
    fun checkUpdate_returnsResultWithoutCrashing() = runBlocking {
        val result = UpdateService.checkUpdate(context)

        assertThat(
            result is UpdateCheckResult.UpdateAvailable ||
                result is UpdateCheckResult.UpToDate ||
                result is UpdateCheckResult.Error
        ).isTrue()
    }

    @Test
    fun checkUpdate_usesInjectedConnection() = runBlocking {
        val connection = TestHttpURLConnection(
            url = URL("http://example.com"),
            responseBody = """{"tag_name":"v9.9.9","body":"On-device release","html_url":"https://example.com"}""",
        )

        val result = UpdateService.checkUpdate(context, openConnection = { connection })

        assertThat(result).isInstanceOf(UpdateCheckResult.UpdateAvailable::class.java)
        assertThat((result as UpdateCheckResult.UpdateAvailable).latestVersion).isEqualTo("9.9.9")
        assertThat(connection.disconnectCalled).isTrue()
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
