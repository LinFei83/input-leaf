package com.inputleaf.android.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
    fun changelogProvider_returnsValidHighlights() {
        val changelog = ChangelogProvider.getChangelog("1.4.1")
        assertThat(changelog.versionName).isEqualTo("1.4.1")
        assertThat(changelog.highlights).isNotEmpty()
    }
}
