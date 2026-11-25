package app.salah

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "الصلاة"
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("prayer", "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH))
        val n = NotificationCompat.Builder(ctx, "prayer")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان الآن وقت صلاة $name")
            .setContentText("حيّ على الصلاة")
            .setAutoCancel(true).build()
        nm.notify(name.hashCode(), n)
        PrayerScheduler.scheduleNext(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) { PrayerScheduler.scheduleNext(ctx) }
}
