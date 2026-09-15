package com.inputleaf.android.util

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.inputleaf.android.network.ConnectResult
import com.inputleaf.android.network.ConnectionTransportPolicy
import com.inputleaf.android.network.InputLeapConnection
import com.inputleaf.android.network.ServerTransport
import com.inputleaf.android.testutil.LOOPBACK_HOST
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.net.ServerSocket

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class InputLeapConnectionAndroidLoggerTest {
    @Test
    fun `default Android logger is used for a closed-port connect`() = runBlocking {
        val closedPort = ServerSocket(0, 50, InetAddress.getByName(LOOPBACK_HOST)).use { it.localPort }
        val connection = InputLeapConnection(
            ip = LOOPBACK_HOST,
            port = closedPort,
            transportPolicy = ConnectionTransportPolicy.PLAIN_ONLY,
            preferredTransport = ServerTransport.PLAIN,
            onCertificate = { true },
        )
        try {
            val result = connection.connect("android", 1920, 1080)
            assertThat(result).isInstanceOf(ConnectResult.Failed::class.java)
        } finally {
            connection.close()
        }
    }
}
