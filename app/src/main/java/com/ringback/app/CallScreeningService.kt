package com.ringback.app

import android.content.Context
import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService

/** Captures the number and direction of a normal cellular call without replacing the phone app. */
class RingBackCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val number = details.handle?.schemeSpecificPart.orEmpty()
        if (number.isNotBlank()) {
            val direction = if (details.callDirection == Call.Details.DIRECTION_INCOMING) "INCOMING" else "OUTGOING"
            val started = details.creationTimeMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
            PendingCallStore.start(this, number, direction, started)
        }

        if (details.callDirection == Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, CallScreeningService.CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build())
        }
    }
}

data class PendingCall(
    val number: String,
    val direction: String,
    val start: Long
)

object PendingCallStore {
    private const val PREFS = "pending_call"
    private const val NUMBER = "number"
    private const val DIRECTION = "direction"
    private const val START = "start"

    fun start(c: Context, number: String, direction: String, start: Long) {
        c.getSharedPreferences(PREFS, 0).edit()
            .putString(NUMBER, number)
            .putString(DIRECTION, direction)
            .putLong(START, start)
            .apply()
    }

    fun read(c: Context): PendingCall? {
        val p = c.getSharedPreferences(PREFS, 0)
        val number = p.getString(NUMBER, null) ?: return null
        val direction = p.getString(DIRECTION, null) ?: return null
        val start = p.getLong(START, 0L)
        if (start == 0L) return null
        return PendingCall(number, direction, start)
    }

    fun clear(c: Context) {
        c.getSharedPreferences(PREFS, 0).edit().clear().apply()
    }
}
