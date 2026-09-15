package com.inputleaf.android.util

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class DeviceIdentityTest {
    @Test
    fun `getMarketingName returns non-empty string`() {
        val name = DeviceIdentity.getMarketingName()
        assertThat(name).isNotEmpty()
    }

    @Test
    fun `getManufacturerName returns capitalized string`() {
        val name = DeviceIdentity.getManufacturerName("samsung")
        assertThat(name).isEqualTo("Samsung")
    }

    @Test
    fun `getInternalModelCode returns model or unknown`() {
        val code = DeviceIdentity.getInternalModelCode("Pixel 7")
        assertThat(code).isEqualTo("Pixel 7")
    }

    @Test
    fun `getAndroidVersion returns formatted android version`() {
        val version = DeviceIdentity.getAndroidVersion("14")
        assertThat(version).isEqualTo("Android 14")
    }

    @Test
    fun `getBrandLogoRes returns valid drawable resource for various brands`() {
        val brands = listOf("google", "samsung", "oneplus", "xiaomi", "redmi", "poco", "realme", "vivo", "oppo", "motorola", "nokia", "nothing", "iqoo", "tecno", "infinix", "asus", "honor", "lava", "micromax", "lenovo", "other")
        for (brand in brands) {
            val res = DeviceIdentity.getBrandLogoRes(brand)
            assertThat(res).isGreaterThan(0)
        }
    }

    @Test
    fun `getBrandColor returns non-null color for various brands`() {
        val brands = listOf("google", "samsung", "oneplus", "xiaomi", "redmi", "poco", "realme", "vivo", "oppo", "motorola", "nokia", "nothing", "iqoo", "tecno", "infinix", "asus", "honor", "lava", "micromax", "lenovo", "other")
        for (brand in brands) {
            val color = DeviceIdentity.getBrandColor(brand)
            assertThat(color).isNotNull()
        }
        assertThat(DeviceIdentity.getBrandColor("pocophone"))
            .isEqualTo(DeviceIdentity.getBrandColor("poco"))
        assertThat(DeviceIdentity.getBrandColor("hmd global"))
            .isEqualTo(DeviceIdentity.getBrandColor("nokia"))
        assertThat(DeviceIdentity.getBrandColor(null))
            .isEqualTo(DeviceIdentity.getBrandColor("android"))
        assertThat(DeviceIdentity.getBrandLogoRes("pocophone"))
            .isEqualTo(DeviceIdentity.getBrandLogoRes("poco"))
        assertThat(DeviceIdentity.getBrandLogoRes("hmd global"))
            .isEqualTo(DeviceIdentity.getBrandLogoRes("nokia"))
        assertThat(DeviceIdentity.getBrandLogoRes(null))
            .isEqualTo(DeviceIdentity.getBrandLogoRes("android"))
    }

    @Test
    fun `null identity fields use documented fallbacks`() {
        assertThat(DeviceIdentity.getManufacturerName(null)).isEqualTo("Android")
        assertThat(DeviceIdentity.getInternalModelCode(null)).isEqualTo("Unknown")
        assertThat(DeviceIdentity.getAndroidVersion(null)).isEqualTo("Android 14")
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class DeviceIdentityMarketingNameTest {
    @Test
    fun `requestMarketingName invokes the callback or falls back without throwing`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        DeviceIdentity.requestMarketingName(context) { name ->
            assertThat(name).isNotEmpty()
        }
        assertThat(DeviceIdentity.getMarketingName()).isNotEmpty()
    }
}
