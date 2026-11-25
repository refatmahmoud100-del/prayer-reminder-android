package app.salah

import android.Manifest
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val ar = Locale("ar")
    private var countdownView: TextView? = null
    private var target: Date? = null
    private val tick = object : Runnable {
        override fun run() {
            val t = target
            if (t != null) {
                val s = ((t.time - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                if (s == 0L) { build(); return }
                countdownView?.text = String.format(ar, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(b)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val perms = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.toTypedArray(), 1)
        build()
        PrayerScheduler.scheduleNext(this)
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        build(); PrayerScheduler.scheduleNext(this)
    }

    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }

    private fun text(s: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false, center: Boolean = true) =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            if (center) gravity = Gravity.CENTER
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = getDrawable(R.drawable.bg_card)
        elevation = dp(4).toFloat()
        setPadding(dp(14), dp(14), dp(14), dp(14))
    }

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { setMargins(dp(16), dp(top), dp(16), 0) }

    private fun build() {
        handler.removeCallbacksAndMessages(null)
        val (nextName, nextTime) = PrayerScheduler.next(this)
        val row = PrayerScheduler.row(this)
        target = nextTime
        val ink = Color.parseColor("#14263A")
        val tf = SimpleDateFormat("hh:mm a", ar)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Header: sky gradient with next prayer
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = getDrawable(R.drawable.bg_header)
            setPadding(dp(16), dp(36), dp(16), dp(56))
        }
        header.addView(text("ميقات", 18f, bold = true))
        header.addView(text("الصلاة القادمة: $nextName", 18f).apply { setPadding(0, dp(28), 0, 0) })
        header.addView(text(tf.format(nextTime), 46f, bold = true))
        countdownView = text("", 22f).also { header.addView(it) }
        val pill = text(if (PrayerScheduler.usedFallback) "📍 دمياط، مصر" else "📍 موقعك الحالي", 14f).apply {
            background = getDrawable(R.drawable.bg_pill); setPadding(dp(16), dp(6), dp(16), dp(6))
        }
        header.addView(pill, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Date card, overlapping the header
        val dateCard = card()
        val greg = SimpleDateFormat("EEEE d MMMM yyyy", ar).format(Date())
        val hijri = try { HijrahDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", ar)) + " هـ" } catch (e: Exception) { "" }
        dateCard.addView(text(greg, 16f, ink, bold = true))
        if (hijri.isNotEmpty()) dateCard.addView(text(hijri, 14f, Color.parseColor("#4A6076")))
        root.addView(dateCard, lp().apply { topMargin = -dp(36) })

        // Prayer row
        val rowCard = card()
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 6f }
        row.forEach { (name, emoji, time) ->
            val isNext = name == nextName
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                if (isNext) background = GradientDrawable().apply { setColor(Color.parseColor("#FFF1D6")); cornerRadius = dp(14).toFloat() }
            }
            cell.addView(text(name, 13f, ink, bold = isNext))
            cell.addView(text(emoji, 22f).apply { setPadding(0, dp(4), 0, dp(4)) })
            cell.addView(text(SimpleDateFormat("hh:mm", ar).format(time), 12f, ink))
            cell.addView(text(SimpleDateFormat("a", ar).format(time), 10f, Color.parseColor("#6B7F92")))
            line.addView(cell, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        rowCard.addView(line)
        root.addView(rowCard, lp(12))

        // Feature grid (placeholders for now)
        val grid = card()
        val items = listOf(
            Triple("المصحف", "📖", "#2E7D6B"), Triple("الأذكار", "🤲", "#2F5D9E"), Triple("المسبحة", "📿", "#3B3B8F"), Triple("القبلة", "🕋", "#4A4A4A"),
            Triple("الحفظ", "🧠", "#B7791F"), Triple("الدعاء", "🌿", "#2B8A8A"), Triple("الختمة", "✅", "#C05621"), Triple("المزيد", "➕", "#7A5C99"))
        items.chunked(4).forEach { chunk ->
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 4f; setPadding(0, dp(8), 0, dp(8)) }
            chunk.forEach { (label, emoji, color) ->
                val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
                val circle = text(emoji, 26f).apply {
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(color)) }
                }
                c.addView(circle, LinearLayout.LayoutParams(dp(60), dp(60)))
                c.addView(text(label, 13f, ink).apply { setPadding(0, dp(6), 0, 0) })
                c.setOnClickListener { Toast.makeText(this, "$label: قريبًا", Toast.LENGTH_SHORT).show() }
                r.addView(c, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            grid.addView(r)
        }
        root.addView(grid, lp(12))
        root.addView(text("نسخة تجريبية", 12f, Color.parseColor("#6B7F92")).apply { setPadding(0, dp(16), 0, dp(24)) })

        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#EEF3F6")); addView(root) })
        handler.post(tick)
    }
}
