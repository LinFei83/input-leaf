package com.inputleaf.android.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class HttpURLConnectionDisconnectTest {

    @Test
    fun useAndDisconnect_disconnectsAfterSuccessfulBlock() {
        val connection = TestHttpURLConnection(URL("http://example.com"))
        val result = connection.useAndDisconnect { "ok" }

        assertThat(result).isEqualTo("ok")
        assertThat(connection.disconnectCalled).isTrue()
    }

    @Test
    fun useAndDisconnect_disconnectsWhenBlockThrows() {
        val connection = TestHttpURLConnection(URL("http://example.com"))

        val thrown = runCatching {
            connection.useAndDisconnect {
                throw IllegalStateException("boom")
            }
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(connection.disconnectCalled).isTrue()
    }

    private class TestHttpURLConnection(url: URL) : HttpURLConnection(url) {
        var disconnectCalled = false

        override fun disconnect() {
            disconnectCalled = true
        }

        override fun connect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getRequestMethod(): String = "GET"

        override fun getResponseCode(): Int = HTTP_OK

        override fun getResponseMessage(): String = "OK"

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
