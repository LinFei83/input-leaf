package com.inputleaf.android.update

data class VersionChangelog(
    val versionName: String,
    val versionCode: Int,
    val highlights: List<String>
)

object ChangelogProvider {

    val RELEASES = listOf(
        VersionChangelog(
            versionName = "1.4.1",
            versionCode = 7,
            highlights = listOf(
                "Added About & Community section with developer portfolio, GitHub, LinkedIn, and contributor credits.",
                "Added update checker with intelligent F-Droid and GitHub release redirection.",
                "Added 'What's New' changelog display upon upgrading.",
                "Enhanced input injection performance and connection stability."
            )
        ),
        VersionChangelog(
            versionName = "1.4.0",
            versionCode = 6,
            highlights = listOf(
                "Added TLS-secured connections with TOFU certificate pinning.",
                "Added client certificate management and custom fingerprints.",
                "Modernized Material 3 UI with enhanced connection cards and favorites."
            )
        )
    )

    fun getChangelog(versionName: String): VersionChangelog {
        val clean = versionName.removePrefix("v").removePrefix("V")
        return RELEASES.firstOrNull { it.versionName == clean } ?: RELEASES.first()
    }
}
