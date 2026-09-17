package com.inputleaf.android.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.inputleaf.android.network.TlsFingerprintManager

@Composable
fun FingerprintDialog(
    fingerprint: String,
    oldFingerprint: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (oldFingerprint != null) "证书已更改！" else "信任此服务器？") },
        text = {
            if (oldFingerprint != null) {
                Text("服务器证书已更改。这可能意味着安全风险。\n\n" +
                    "旧：${formatFingerprintForDisplay(oldFingerprint)}\n\n" +
                    "新：${formatFingerprintForDisplay(fingerprint)}")
            } else {
                Text("请验证此指纹与 PC 上 Deskflow 显示的指纹一致：\n\n" +
                    formatFingerprintForDisplay(fingerprint), fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("信任") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun LocalFingerprintDialog(
    fingerprint: String,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
    onImport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("此设备的指纹") },
        text = {
            Text(
                "当 Deskflow 请求信任新客户端时，请与此指纹进行比对。\n\n" +
                    formatFingerprintForDisplay(fingerprint),
                fontFamily = FontFamily.Monospace,
            )
        },
        confirmButton = {
            TextButton(onClick = onImport) { Text("导入 PKCS12") }
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = onRegenerate) { Text("重新生成") }
        },
    )
}

private fun formatFingerprintForDisplay(fingerprint: String): String =
    TlsFingerprintManager.formatFingerprint(fingerprint)
