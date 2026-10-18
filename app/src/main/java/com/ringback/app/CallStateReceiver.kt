package com.ringback.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.TelephonyManager
import java.util.Calendar

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val app = context.applicationContext
        val runtime = app.getSharedPreferences("call_runtime", 0)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> runtime.edit().putBoolean("ringing", true).apply()
            TelephonyManager.EXTRA_STATE_OFFHOOK -> runtime.edit()
                .putBoolean("offhook", true)
                .putLong("offhook_at", System.currentTimeMillis())
                .apply()
            TelephonyManager.EXTRA_STATE_IDLE -> finishCall(app, runtime)
        }
    }

    private fun finishCall(app: Context, runtime: android.content.SharedPreferences) {
        val hadOffhook = runtime.getBoolean("offhook", false)
        val hadRinging = runtime.getBoolean("ringing", false)
        val offhookAt = runtime.getLong("offhook_at", 0L)
        runtime.edit().clear().apply()

        val pending = PendingCallStore.read(app) ?: return
        if (System.currentTimeMillis() - pending.start > 2 * 60 * 60 * 1000L) {
            PendingCallStore.clear(app); return
        }

        val type = when {
            pending.direction == "OUTGOING" -> "OUTGOING"
            pending.direction == "INCOMING" && hadRinging && !hadOffhook -> "MISSED"
            else -> "INCOMING"
        }
        if (!SettingsStore.enabled(app, type.lowercase())) { PendingCallStore.clear(app); return }
        if (!withinWorkSchedule(app)) { PendingCallStore.clear(app); return }
        if (!Settings.canDrawOverlays(app)) { PendingCallStore.clear(app); return }

        val name = contactName(app, pending.number)
        val durationStart = if (hadOffhook && offhookAt > 0) offhookAt else System.currentTimeMillis()
        val duration = if (type == "MISSED") 0L else ((System.currentTimeMillis() - durationStart) / 1000L).coerceAtLeast(0L)
        val record = CallRecord(number = pending.number, name = name, type = type, startedAt = pending.start, durationSec = duration)
        PendingCallStore.clear(app)

        val go = goAsync()
        Handler(Looper.getMainLooper()).post {
            try { CallOverlay.show(app, record) } finally { go.finish() }
        }
    }

    private fun contactName(c: Context, number: String): String {
        if (c.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return number
        return try {
            c.contentResolver.query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else number
            } ?: number
        } catch (_: Exception) { number }
    }

    private fun withinWorkSchedule(c: Context): Boolean {
        val p = SettingsStore.p(c)
        if (!p.getBoolean("schedule_enabled", false)) return true
        val cal = Calendar.getInstance()
        val dayKey = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "mon"; Calendar.TUESDAY -> "tue"; Calendar.WEDNESDAY -> "wed"; Calendar.THURSDAY -> "thu"
            Calendar.FRIDAY -> "fri"; Calendar.SATURDAY -> "sat"; else -> "sun"
        }
        if (!p.getBoolean("day_$dayKey", dayKey !in listOf("sat", "sun"))) return false
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = p.getInt("work_start", 480); val end = p.getInt("work_end", 1080)
        return if (start <= end) now in start until end else now >= start || now < end
    }
}

object SettingsStore {
    fun p(c: Context) = c.getSharedPreferences("settings", 0)
    fun enabled(c: Context, k: String) = p(c).getBoolean("enabled_$k", true)
    fun reminder(c: Context) = p(c).getInt("reminder_minutes", 60)
    fun autoHide(c: Context) = p(c).getInt("auto_hide_seconds", 15)
}
