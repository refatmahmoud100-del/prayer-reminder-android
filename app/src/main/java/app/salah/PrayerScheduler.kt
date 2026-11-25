package app.salah

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import java.util.*

object PrayerScheduler {
    // Fallback: Damietta, Egypt
    private const val DEF_LAT = 31.4165
    private const val DEF_LON = 31.8133
    val names = mapOf(Prayer.FAJR to "الفجر", Prayer.DHUHR to "الظهر", Prayer.ASR to "العصر",
        Prayer.MAGHRIB to "المغرب", Prayer.ISHA to "العشاء")

    @SuppressLint("MissingPermission")
    fun coordinates(ctx: Context): Coordinates {
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
            if (loc != null) return Coordinates(loc.latitude, loc.longitude)
        } catch (_: Exception) {}
        return Coordinates(DEF_LAT, DEF_LON)
    }

    fun timesFor(ctx: Context, day: Date): PrayerTimes {
        val params = CalculationMethod.EGYPTIAN.parameters
        return PrayerTimes(coordinates(ctx), DateComponents.from(day), params)
    }

    fun today(ctx: Context): List<Pair<String, Date>> {
        val t = timesFor(ctx, Date())
        return listOf(Prayer.FAJR to t.fajr, Prayer.DHUHR to t.dhuhr, Prayer.ASR to t.asr,
            Prayer.MAGHRIB to t.maghrib, Prayer.ISHA to t.isha).map { names[it.first]!! to it.second }
    }

    /** Schedules the next upcoming prayer alarm. Called on app open, boot, and after each alarm. */
    fun scheduleNext(ctx: Context) {
        val now = Date()
        val cal = Calendar.getInstance()
        val candidates = (0..1).flatMap { d ->
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, d) }.time
            val t = timesFor(ctx, day)
            listOf(Prayer.FAJR to t.fajr, Prayer.DHUHR to t.dhuhr, Prayer.ASR to t.asr,
                Prayer.MAGHRIB to t.maghrib, Prayer.ISHA to t.isha)
        }
        val next = candidates.firstOrNull { it.second.after(now) } ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(ctx, 1,
            Intent(ctx, PrayerReceiver::class.java).putExtra("name", names[next.first]),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val at = next.second.time
        if (android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}
