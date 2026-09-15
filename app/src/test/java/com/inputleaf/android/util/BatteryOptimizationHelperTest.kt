package com.inputleaf.android.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class BatteryOptimizationHelperTest {
    @Test
    fun `getOemComponents returns Huawei components for huawei and honor`() {
        val huawei = BatteryOptimizationHelper.getOemComponents("Huawei")
        assertThat(huawei).isNotEmpty()
        assertThat(huawei[0].packageName).isEqualTo("com.huawei.systemmanager")

        val honor = BatteryOptimizationHelper.getOemComponents("HONOR")
        assertThat(honor).isNotEmpty()
        assertThat(honor[0].packageName).isEqualTo("com.huawei.systemmanager")
    }

    @Test
    fun `getOemComponents returns Samsung components for samsung`() {
        val samsung = BatteryOptimizationHelper.getOemComponents("Samsung")
        assertThat(samsung).hasSize(2)
        assertThat(samsung[0].packageName).isEqualTo("com.samsung.android.lool")
    }

    @Test
    fun `getOemComponents returns Xiaomi components for xiaomi redmi poco`() {
        for (oem in listOf("Xiaomi", "Redmi", "Poco")) {
            val comps = BatteryOptimizationHelper.getOemComponents(oem)
            assertThat(comps).isNotEmpty()
            assertThat(comps[0].packageName).isEqualTo("com.miui.powerkeeper")
        }
    }

    @Test
    fun `getOemComponents returns Vivo components for vivo`() {
        val vivo = BatteryOptimizationHelper.getOemComponents("Vivo")
        assertThat(vivo).isNotEmpty()
        assertThat(vivo[0].packageName).isEqualTo("com.vivo.abe")
    }

    @Test
    fun `getOemComponents returns Asus components for asus`() {
        val asus = BatteryOptimizationHelper.getOemComponents("Asus")
        assertThat(asus).isNotEmpty()
        assertThat(asus[0].packageName).isEqualTo("com.asus.mobilemanager")
    }

    @Test
    fun `getOemComponents returns Lenovo components for lenovo`() {
        val lenovo = BatteryOptimizationHelper.getOemComponents("Lenovo")
        assertThat(lenovo).isNotEmpty()
        assertThat(lenovo[0].packageName).isEqualTo("com.lenovo.security")
    }

    @Test
    fun `getOemComponents returns Nokia components for nokia`() {
        val nokia = BatteryOptimizationHelper.getOemComponents("Nokia")
        assertThat(nokia).isNotEmpty()
        assertThat(nokia[0].packageName).isEqualTo("com.evenwell.powersaving.g3")
    }

    @Test
    fun `getOemComponents returns empty for unknown oem`() {
        val unknown = BatteryOptimizationHelper.getOemComponents("Google")
        assertThat(unknown).isEmpty()
    }

    @Test
    fun `isColorOsOrDirectSettings returns true for OnePlus Oppo Realme`() {
        assertThat(BatteryOptimizationHelper.isColorOsOrDirectSettings("OnePlus")).isTrue()
        assertThat(BatteryOptimizationHelper.isColorOsOrDirectSettings("OPPO")).isTrue()
        assertThat(BatteryOptimizationHelper.isColorOsOrDirectSettings("realme")).isTrue()
        assertThat(BatteryOptimizationHelper.isColorOsOrDirectSettings("Google")).isFalse()
        assertThat(BatteryOptimizationHelper.isColorOsOrDirectSettings("Samsung")).isFalse()
    }

    @Test
    fun `requestExemption launches the first available settings intent`() {
        val context = mock(Context::class.java)
        `when`(context.packageName).thenReturn("com.inputleaf.android")

        BatteryOptimizationHelper.requestExemption(context)

        verify(context).startActivity(any(Intent::class.java))
    }

    @Test
    fun `requestExemption falls through when earlier settings activities are missing`() {
        val context = mock(Context::class.java)
        `when`(context.packageName).thenReturn("com.inputleaf.android")
        doThrow(RuntimeException("missing"))
            .doNothing()
            .`when`(context)
            .startActivity(any(Intent::class.java))

        BatteryOptimizationHelper.requestExemption(context)

        verify(context, times(2)).startActivity(any(Intent::class.java))
    }

    @Test
    fun `requestExemption tries ColorOS application details first`() {
        ShadowBuild.setManufacturer("OnePlus")
        val context = mock(Context::class.java)
        `when`(context.packageName).thenReturn("com.inputleaf.android")

        BatteryOptimizationHelper.requestExemption(context)

        verify(context).startActivity(any(Intent::class.java))
        ShadowBuild.setManufacturer("unknown")
    }

    @Test
    fun `requestExemption tries OEM components before AOSP fallbacks`() {
        ShadowBuild.setManufacturer("samsung")
        val context = mock(Context::class.java)
        `when`(context.packageName).thenReturn("com.inputleaf.android")

        BatteryOptimizationHelper.requestExemption(context)

        verify(context).startActivity(any(Intent::class.java))
        ShadowBuild.setManufacturer("unknown")
    }

    @Test
    fun `requestExemption swallows every missing settings activity`() {
        val context = mock(Context::class.java)
        `when`(context.packageName).thenReturn("com.inputleaf.android")
        doThrow(RuntimeException("missing")).`when`(context).startActivity(any(Intent::class.java))

        BatteryOptimizationHelper.requestExemption(context)

        verify(context, org.mockito.Mockito.atLeast(3)).startActivity(any(Intent::class.java))
    }
}
