package com.vesper.mobile.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vesper.mobile.R

class NotificationHelper(private val context: Context) {

    enum class Channel(val id: String, val title: String, val description: String) {
        INTAKE("vesper_intake", "Intake", "Inbox and drive intake events"),
        REVIEW("vesper_review", "Review", "Staging review and attention"),
        RELEASE("vesper_release", "Release", "Release candidate and publish events"),
        SIGNING("vesper_signing", "Signing", "Unsigned export / signed import handoff"),
        SYSTEM("vesper_system", "System", "Mortis health and session events"),
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Channel.entries.forEach { ch ->
            val existing = mgr.getNotificationChannel(ch.id)
            if (existing == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(ch.id, ch.title, NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = ch.description
                        setShowBadge(true)
                    },
                )
            }
        }
    }

    fun notify(channel: Channel, id: Int, title: String, body: String) {
        ensureChannels()
        val n = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, n)
        }
    }
}
