package app.salah

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val perms = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 23) requestPermissions(perms.toTypedArray(), 1)
        render()
        PrayerScheduler.scheduleNext(this)
    }
    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        render(); PrayerScheduler.scheduleNext(this)
    }
    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48); gravity = Gravity.CENTER_HORIZONTAL }
        root.addView(TextView(this).apply { text = "مواقيت الصلاة اليوم"; textSize = 26f; gravity = Gravity.CENTER })
        val f = SimpleDateFormat("hh:mm a", Locale("ar"))
        PrayerScheduler.today(this).forEach { (n, t) ->
            root.addView(TextView(this).apply { text = "$n   ${f.format(t)}"; textSize = 22f; setPadding(0, 32, 0, 0) })
        }
        setContentView(root)
    }
}
