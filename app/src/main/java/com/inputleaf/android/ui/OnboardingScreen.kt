package com.inputleaf.android.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inputleaf.android.R

@Composable
fun OnboardingScreen(
    shizukuStatus: ShizukuStatus,
    accessibilityAvailable: Boolean,
    canDrawOverlays: Boolean,
    batteryOptimizationExempt: Boolean,
    imeEnabledAndSelected: Boolean,
    onRequestShizukuPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestImeSetup: () -> Unit,
    onComplete: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 6
    val context = LocalContext.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Progress indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index <= currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            // Page content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (currentPage) {
                    0 -> WelcomePage()
                    1 -> PermissionPage(
                        icon = Icons.Rounded.Security,
                        title = "Shizuku 设置（可选）",
                        description = "Shizuku 可让 Input Leaf 在系统层面注入鼠标和键盘事件，是最强大的输入方式。如果你更倾向使用无障碍服务，可以跳过此页。",
                        whyNeeded = "Shizuku 无需真正 root 设备，即可提供低延迟、等效于 root 的输入注入。如果无法使用 Shizuku，你可以使用下一页的无障碍服务。",
                        isGranted = shizukuStatus == ShizukuStatus.READY,
                        statusText = when (shizukuStatus) {
                            ShizukuStatus.READY -> "就绪 ✓"
                            ShizukuStatus.NOT_INSTALLED -> "未安装"
                            ShizukuStatus.NOT_RUNNING -> "未运行"
                            ShizukuStatus.PERMISSION_REQUIRED -> "需要授权"
                            ShizukuStatus.CHECKING -> "检查中…"
                        },
                        actionLabel = when (shizukuStatus) {
                            ShizukuStatus.NOT_INSTALLED -> "安装 Shizuku"
                            ShizukuStatus.NOT_RUNNING -> "打开 Shizuku"
                            ShizukuStatus.PERMISSION_REQUIRED -> "授予权限"
                            else -> null
                        },
                        onAction = when (shizukuStatus) {
                            ShizukuStatus.NOT_INSTALLED -> ({
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")))
                            })
                            ShizukuStatus.NOT_RUNNING -> ({
                                context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let {
                                    context.startActivity(it)
                                }
                                Unit
                            })
                            ShizukuStatus.PERMISSION_REQUIRED -> onRequestShizukuPermission
                            else -> ({})
                        }
                    )
                    2 -> PermissionPage(
                        icon = Icons.Rounded.Accessibility,
                        title = "无障碍服务",
                        description = "启用 Input Leaf 的无障碍服务——一种无需 root、无需 Shizuku 即可注入触摸事件的方式。适用于任何 Android 设备。",
                        whyNeeded = "无障碍服务让 Input Leaf 无需 Shizuku 即可模拟点击。它无需任何额外应用，即可在原版 Android 上运行。",
                        isGranted = accessibilityAvailable,
                        statusText = if (accessibilityAvailable) "已启用 ✓" else "已禁用",
                        actionLabel = if (!accessibilityAvailable) "打开无障碍设置" else null,
                        onAction = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                val comp = ComponentName(
                                    context.packageName,
                                    com.inputleaf.android.inject.AccessibilityInputService::class.java.name
                                )
                                putExtra(":settings:show_fragment_args",
                                    android.os.Bundle().apply { putString(":settings:fragment_args_key", comp.flattenToString()) })
                            }
                            context.startActivity(intent)
                        }
                    )
                    3 -> PermissionPage(
                        icon = Icons.Default.Warning,
                        title = "虚拟键盘",
                        description = "注入 Ctrl+C、Alt+Tab 等硬件键盘快捷键时需要此项。",
                        whyNeeded = "为了在没有 Shizuku 的情况下完全模拟物理键盘，Input Leaf 使用自定义虚拟键盘。你需要启用它，并将其设为当前使用的键盘。",
                        isGranted = imeEnabledAndSelected,
                        statusText = if (imeEnabledAndSelected) "已选择 ✓" else "未选择",
                        actionLabel = if (!imeEnabledAndSelected) "选择键盘" else null,
                        onAction = onRequestImeSetup
                    )
                    4 -> PermissionPage(
                        icon = Icons.Rounded.Visibility,
                        title = "悬浮窗权限",
                        description = "允许 Input Leaf 在电脑鼠标移到此设备时，在你的屏幕上显示光标。",
                        whyNeeded = "Android 要求获得明确授权才能在其他应用上层绘制内容。需要此权限来显示光标悬浮层，以便你看到鼠标指针在手机上的位置。",
                        isGranted = canDrawOverlays,
                        statusText = if (canDrawOverlays) "已授予 ✓" else "未授予",
                        actionLabel = if (!canDrawOverlays) "授予权限" else null,
                        onAction = onRequestOverlayPermission
                    )
                    5 -> PermissionPage(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "电池优化",
                        description = "防止 Android 在手机休眠时断开连接。\n\n前往：电池用量 → 允许后台活动",
                        whyNeeded = "Android 会为节省电量而激进地结束后台应用。将 Input Leaf 设为豁免可确保屏幕关闭时 KVM 连接依然保持。",
                        isGranted = batteryOptimizationExempt,
                        statusText = if (batteryOptimizationExempt) "已豁免 ✓" else "未豁免",
                        actionLabel = if (!batteryOptimizationExempt) "打开应用信息" else null,
                        onAction = onRequestBatteryOptimization
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentPage > 0) {
                    OutlinedButton(onClick = { currentPage-- }) {
                        Text("返回")
                    }
                } else {
                    TextButton(onClick = onComplete) {
                        Text("跳过")
                    }
                }

                if (currentPage < totalPages - 1) {
                    Button(onClick = { currentPage++ }) {
                        Text("下一步")
                    }
                } else {
                    Button(onClick = onComplete) {
                        Text("开始使用")
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "Input Leaf 标志",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "欢迎使用 Input Leaf",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "将电脑的键盘和鼠标无缝共享给你的 Android 设备。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                FeatureItem(Icons.Rounded.Mouse, "鼠标共享", "将光标从电脑移动到手机")
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem(Icons.Rounded.Keyboard, "键盘共享", "使用电脑的键盘在手机上输入")
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem(Icons.Rounded.Lock, "安全连接", "TLS 加密，首次使用即信任")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Input Leap 的 Android 扩展——开源 KVM",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionPage(
    icon: ImageVector,
    title: String,
    description: String,
    whyNeeded: String,
    isGranted: Boolean,
    statusText: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    var showWhy by remember { mutableStateOf(false) }
    
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isGranted) MaterialTheme.colorScheme.tertiaryContainer
                      else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "bg_color"
    )
    val iconTintColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isGranted) MaterialTheme.colorScheme.onTertiaryContainer
                      else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "icon_tint_color"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Icon with status
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = bgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGranted) Icons.Rounded.CheckCircle else icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = iconTintColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status chip
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = iconTintColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // "Why do we need this?" expandable
        TextButton(onClick = { showWhy = !showWhy }) {
            Icon(
                imageVector = if (showWhy) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("为什么需要这项权限？")
        }

        AnimatedVisibility(
            visible = showWhy,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = whyNeeded,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action button
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel)
            }
        } else if (isGranted) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "全部就绪！",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
