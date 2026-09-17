package com.inputleaf.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.inputleaf.android.model.ConnectionState
import com.inputleaf.android.ui.MainActivity

const val CHANNEL_ID = "inputleaf_status"
const val NOTIF_ID = 1001
const val ACTION_DISCONNECT = "com.inputleaf.android.DISCONNECT"

object NotificationHelper {
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Input-Leaf 状态", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "显示 Input-Leaf 的连接状态" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(context: Context, state: ConnectionState): Notification {
        val text = when (state) {
            is ConnectionState.Active -> "已激活 · ${state.serverName}"
            is ConnectionState.Idle   -> "空闲 · ${state.serverName}"
            is ConnectionState.Connecting, is ConnectionState.Handshaking -> "连接中…"
            is ConnectionState.Disconnected -> "已断开"
        }
        val tapIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectIntent = PendingIntent.getService(
            context, 0,
            Intent(context, ConnectionService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("Input-Leaf")
            .setContentText(text)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "断开连接", disconnectIntent)
            .build()
    }
}
