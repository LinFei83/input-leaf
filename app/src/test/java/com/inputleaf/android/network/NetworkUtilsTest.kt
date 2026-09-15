package com.inputleaf.android.network

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class NetworkUtilsTest {
    @Test
    fun `isPrivateIP identifies 10 dot 0 dot 0 dot 0 range`() {
        assertThat(NetworkUtils.isPrivateIP("10.0.0.1")).isTrue()
        assertThat(NetworkUtils.isPrivateIP("10.255.255.254")).isTrue()
    }

    @Test
    fun `isPrivateIP identifies 172 dot 16 to 31 range`() {
        assertThat(NetworkUtils.isPrivateIP("172.16.0.1")).isTrue()
        assertThat(NetworkUtils.isPrivateIP("172.24.10.5")).isTrue()
        assertThat(NetworkUtils.isPrivateIP("172.31.255.255")).isTrue()
        assertThat(NetworkUtils.isPrivateIP("172.15.0.1")).isFalse()
        assertThat(NetworkUtils.isPrivateIP("172.32.0.1")).isFalse()
    }

    @Test
    fun `isPrivateIP identifies 192 dot 168 range`() {
        assertThat(NetworkUtils.isPrivateIP("192.168.1.1")).isTrue()
        assertThat(NetworkUtils.isPrivateIP("192.168.0.254")).isTrue()
        assertThat(NetworkUtils.isPrivateIP("192.169.1.1")).isFalse()
    }

    @Test
    fun `isPrivateIP rejects public and invalid IPs`() {
        assertThat(NetworkUtils.isPrivateIP("8.8.8.8")).isFalse()
        assertThat(NetworkUtils.isPrivateIP("1.1.1.1")).isFalse()
        assertThat(NetworkUtils.isPrivateIP("invalid")).isFalse()
        assertThat(NetworkUtils.isPrivateIP("192.168.1")).isFalse()
        assertThat(NetworkUtils.isPrivateIP("192.168.1.abc")).isFalse()
        assertThat(NetworkUtils.isPrivateIP("256.1.1.1")).isFalse()
        assertThat(NetworkUtils.isPrivateIP("10.0.0.256")).isFalse()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class NetworkUtilsLocalIpTest {
    @Test
    fun `getLocalIpAddress probes interfaces without throwing`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val ip = NetworkUtils.getLocalIpAddress(app)
        if (ip != null) {
            assertThat(ip).contains(".")
        }
    }
}
