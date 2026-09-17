package com.inputleaf.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun AdbSetupWizard(
    adbPushCommand: String,
    adbStartCommand: String,
    isVerifying: Boolean,
    verifyResult: Boolean?,
    onVerify: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置 ADB") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("若要注入键盘和鼠标事件，请在 PC 上运行以下命令：")

                listOf(
                    "1. 启用 USB 调试（设置 → 开发者选项）",
                    "2. 连接 USB 数据线",
                    "3. 在 PC 上运行："
                ).forEach { Text(it) }

                CommandBlock(adbPushCommand) {
                    clipboard.setText(AnnotatedString(adbPushCommand))
                }
                CommandBlock(adbStartCommand) {
                    clipboard.setText(AnnotatedString(adbStartCommand))
                }

                when {
                    isVerifying -> Row {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("正在验证…")
                    }
                    verifyResult == true  -> Text("✅ UHID 服务器正在运行！")
                    verifyResult == false -> Text("❌ 未检测到 UHID 服务器。请检查上面的命令。")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onVerify, enabled = !isVerifying) { Text("验证") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("跳过") } }
    )
}

@Composable
private fun CommandBlock(command: String, onCopy: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            Text(command, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            TextButton(onClick = onCopy) { Text("复制") }
        }
    }
}
