package com.inputleaf.android.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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
    fun checkUpdate_returnsResultWithoutCrashing() = runBlocking {
        val result = UpdateService.checkUpdate(context)

        assertThat(
            result is UpdateCheckResult.UpdateAvailable ||
                result is UpdateCheckResult.UpToDate ||
                result is UpdateCheckResult.Error
        ).isTrue()
    }
}
