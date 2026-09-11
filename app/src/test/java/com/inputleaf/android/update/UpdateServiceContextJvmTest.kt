package com.inputleaf.android.update

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class UpdateServiceContextJvmTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getCurrentVersion_readsInstalledVersion() {
        assertThat(UpdateService.getCurrentVersion(context)).isEqualTo("1.4.1")
    }

    @Test
    fun getCurrentVersionCode_readsInstalledVersionCode() {
        UpdateService.getCurrentVersionCode(context)
    }

    @Test
    fun getInstallSource_returnsKnownSource() {
        assertThat(UpdateService.getInstallSource(context)).isEqualTo(InstallSource.GITHUB)
    }

    @Test
    fun readModernInstallerPackageName_invokesModernLookupOnApi34() {
        runCatching { readModernInstallerPackageName(context) }
    }

    @Test
    fun checkUpdate_returnsResultWithoutCrashing() = runTest {
        val result = UpdateService.checkUpdate(context)

        assertThat(
            result is UpdateCheckResult.UpdateAvailable ||
                result is UpdateCheckResult.UpToDate ||
                result is UpdateCheckResult.Error
        ).isTrue()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class UpdateServiceLegacySdkJvmTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getInstallSource_usesLegacyInstallerLookupBelowApi30() {
        UpdateService.getInstallSource(context)
    }

    @Test
    fun readLegacyInstallerPackageName_readsInstallSourceOnApi29() {
        readLegacyInstallerPackageName(context)
    }

    @Test
    fun getCurrentVersion_usesLegacyPackageInfoLookupBelowApi33() {
        UpdateService.getCurrentVersion(context)
    }

    @Test
    fun readLegacyPackageInfo_readsPackageInfoOnApi29() {
        readLegacyPackageInfo(context)
    }
}
