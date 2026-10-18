package com.ringback.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CallRecord(
    val id: Long = 0,
    val number: String,
    val name: String,
    val type: String,
    val startedAt: Long,
    val durationSec: Long,
    val note: String = "",
    val reminderAt: Long? = null,
    val status: String = "DONE"
)

class CallDb(c: Context) : SQLiteOpenHelper(c, "ringback.db", null, 2) {
    override fun onCreate(d: SQLiteDatabase) {
        d.execSQL("CREATE TABLE calls(id INTEGER PRIMARY KEY AUTOINCREMENT,number TEXT,name TEXT,type TEXT,startedAt INTEGER,durationSec INTEGER,note TEXT,reminderAt INTEGER,status TEXT DEFAULT 'DONE')")
    }

    override fun onUpgrade(d: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) d.execSQL("ALTER TABLE calls ADD COLUMN status TEXT DEFAULT 'DONE'")
    }

    fun insert(r: CallRecord): Long {
        val v = values(r)
        return writableDatabase.insert("calls", null, v)
    }

    fun updateReminder(id: Long, at: Long?, note: String? = null) {
        val v = ContentValues().apply {
            if (at == null) putNull("reminderAt") else put("reminderAt", at)
            put("status", if (at == null) "DONE" else "PENDING")
            if (note != null) put("note", note)
        }
        writableDatabase.update("calls", v, "id=?", arrayOf(id.toString()))
    }

    fun markReminderFired(id: Long) {
        val v = ContentValues().apply { put("status", "FIRED") }
        writableDatabase.update("calls", v, "id=?", arrayOf(id.toString()))
    }

    fun delete(id: Long) { writableDatabase.delete("calls", "id=?", arrayOf(id.toString())) }

    fun get(id: Long): CallRecord? = readableDatabase.query("calls", null, "id=?", arrayOf(id.toString()), null, null, null).use { c ->
        if (c.moveToFirst()) fromCursor(c) else null
    }

    fun pending(): List<CallRecord> = query("status='PENDING' AND reminderAt IS NOT NULL", "reminderAt ASC")

    fun all(): List<CallRecord> = query(null, "startedAt DESC")

    private fun query(where: String?, order: String): List<CallRecord> {
        val out = mutableListOf<CallRecord>()
        readableDatabase.query("calls", null, where, null, null, null, order).use { c -> while (c.moveToNext()) out += fromCursor(c) }
        return out
    }

    private fun fromCursor(c: android.database.Cursor): CallRecord = CallRecord(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        number = c.getString(c.getColumnIndexOrThrow("number")),
        name = c.getString(c.getColumnIndexOrThrow("name")),
        type = c.getString(c.getColumnIndexOrThrow("type")),
        startedAt = c.getLong(c.getColumnIndexOrThrow("startedAt")),
        durationSec = c.getLong(c.getColumnIndexOrThrow("durationSec")),
        note = c.getString(c.getColumnIndexOrThrow("note")) ?: "",
        reminderAt = if (c.isNull(c.getColumnIndexOrThrow("reminderAt"))) null else c.getLong(c.getColumnIndexOrThrow("reminderAt")),
        status = c.getString(c.getColumnIndexOrThrow("status")) ?: "DONE"
    )

    private fun values(r: CallRecord) = ContentValues().apply {
        put("number", r.number); put("name", r.name); put("type", r.type); put("startedAt", r.startedAt)
        put("durationSec", r.durationSec); put("note", r.note)
        if (r.reminderAt == null) putNull("reminderAt") else put("reminderAt", r.reminderAt)
        put("status", r.status)
    }
}
