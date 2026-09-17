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
                "新增「关于与社区」板块，包含开发者主页、GitHub、LinkedIn 与贡献者名单。",
                "新增更新检查功能，可智能跳转到 F-Droid 或 GitHub 发布页。",
                "升级后新增「新功能」更新日志展示。",
                "提升输入注入性能与连接稳定性。"
            )
        ),
        VersionChangelog(
            versionName = "1.4.0",
            versionCode = 6,
            highlights = listOf(
                "新增基于 TOFU 证书固定（pinning）的 TLS 加密连接。",
                "新增客户端证书管理与自定义指纹功能。",
                "采用 Material 3 现代化界面，改进连接卡片与收藏功能。"
            )
        )
    )

    fun getChangelog(versionName: String): VersionChangelog {
        val clean = versionName.removePrefix("v").removePrefix("V")
        return RELEASES.firstOrNull { it.versionName == clean } ?: RELEASES.first()
    }
}
