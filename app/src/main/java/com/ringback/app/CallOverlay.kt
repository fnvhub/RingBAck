package com.ringback.app

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import java.util.Calendar

object CallOverlay {
    private var wm: WindowManager? = null
    private var view: View? = null

    fun show(c: Context, r: CallRecord) {
        if (!Settings.canDrawOverlays(c)) return
        Handler(Looper.getMainLooper()).post {
            remove(); val a = c.applicationContext
            wm = a.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val box = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(a, 18), dp(a, 16), dp(a, 18), dp(a, 16)); background = bg(Color.rgb(0, 82, 68), 28, Color.rgb(60, 170, 140)) }
            val top = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
            top.addView(t(a, when (r.type) { "OUTGOING" -> "LLAMADA SALIENTE FINALIZADA"; "MISSED" -> "LLAMADA PERDIDA"; else -> "LLAMADA ENTRANTE FINALIZADA" }, 14, Color.rgb(93,234,190), true), LinearLayout.LayoutParams(0, -2, 1f))
            top.addView(t(a, "✕", 24, Color.WHITE, true).apply { gravity = Gravity.CENTER; setOnClickListener { remove() } }, LinearLayout.LayoutParams(dp(a, 44), dp(a, 44)))
            box.addView(top)
            box.addView(t(a, r.name, 24, Color.WHITE, true)); box.addView(t(a, r.number, 15, Color.rgb(180,215,205)))
            box.addView(t(a, "✦  NOTA DE LLAMADA", 14, Color.rgb(126,235,194), true).apply { setPadding(0, dp(a, 20), 0, dp(a, 8)) })
            val note = EditText(a).apply { hint = "¿De qué trataba la llamada?"; setHintTextColor(Color.rgb(180,215,205)); setTextColor(Color.WHITE); minLines = 2; gravity = Gravity.TOP; background = bg(Color.rgb(10,98,82), 20, Color.rgb(55,150,125)); setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 10)) }
            box.addView(note, LinearLayout.LayoutParams(-1, dp(a, 82)))
            box.addView(t(a, "⏱  ¿CUÁNDO DEVOLVER LA LLAMADA?", 14, Color.rgb(126,235,194), true).apply { setPadding(0, dp(a, 18), 0, dp(a, 8)) })

            val grid = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            var selected = SettingsStore.reminder(a)
            val buttons = mutableMapOf<Int, TextView>()
            val vals = listOf(5, 10, 15, 30, 60, 120, 240)
            fun refresh() { buttons.forEach { (m, b) -> b.background = bg(if (m == selected) Color.rgb(0,180,128) else Color.rgb(12,92,78), 24, Color.rgb(50,155,130)) } }
            vals.chunked(4).forEach { rowVals ->
                val row = LinearLayout(a)
                rowVals.forEach { m ->
                    val b = t(a, fmt(m), 14, Color.WHITE, true).apply { gravity = Gravity.CENTER; setOnClickListener { selected = m; refresh() } }
                    buttons[m] = b; row.addView(b, LinearLayout.LayoutParams(0, dp(a, 46), 1f).apply { setMargins(3,3,3,3) })
                }; grid.addView(row)
            }
            val extra = LinearLayout(a)
            extra.addView(t(a, "Mañana misma hora", 14, Color.WHITE, true).apply { gravity = Gravity.CENTER; background = bg(Color.rgb(12,92,78),24,Color.rgb(50,155,130)); setOnClickListener { selected = 1440; refresh() } }, LinearLayout.LayoutParams(0, dp(a, 46), 1f).apply { setMargins(3,3,3,3) })
            extra.addView(t(a, "Fecha y hora…", 14, Color.WHITE, true).apply { gravity = Gravity.CENTER; background = bg(Color.rgb(12,92,78),24,Color.rgb(50,155,130)); setOnClickListener { chooseCustom(a, r, note.text.toString()) } }, LinearLayout.LayoutParams(0, dp(a, 46), 1f).apply { setMargins(3,3,3,3) })
            grid.addView(extra); refresh(); box.addView(grid)

            val actions = LinearLayout(a)
            actions.addView(t(a, "Ahora no", 15, Color.WHITE, true).apply { gravity = Gravity.CENTER; background = bg(Color.rgb(8,65,55),24,Color.rgb(50,110,98)); setOnClickListener { save(a,r,note.text.toString(),null) } }, LinearLayout.LayoutParams(0, dp(a, 54), 1f).apply { setMargins(0,10,5,0) })
            actions.addView(t(a, "✓  Guardar", 15, Color.WHITE, true).apply { gravity = Gravity.CENTER; background = bg(Color.rgb(0,180,128),24,Color.rgb(70,225,175)); setOnClickListener { save(a,r,note.text.toString(),System.currentTimeMillis()+selected*60000L) } }, LinearLayout.LayoutParams(0, dp(a,54), 1.2f).apply { setMargins(5,10,0,0) })
            box.addView(actions)
            val lp = WindowManager.LayoutParams(minOf(dp(a,620), a.resources.displayMetrics.widthPixels-dp(a,24)), WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_DIM_BEHIND, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.CENTER; dimAmount=.30f }
            try { wm!!.addView(box,lp); view=box } catch (_:Exception) { return@post }
            val sec=SettingsStore.autoHide(a); if(sec>0) Handler(Looper.getMainLooper()).postDelayed({remove()},sec*1000L)
        }
    }

    private fun chooseCustom(c: Context, r: CallRecord, note: String) {
        val now=Calendar.getInstance()
        val date=DatePickerDialog(c, { _,y,m,d ->
            val time=TimePickerDialog(c, { _,h,min ->
                val at=Calendar.getInstance().apply{set(y,m,d,h,min,0);set(Calendar.MILLISECOND,0)}.timeInMillis
                if(at<=System.currentTimeMillis()){Toast.makeText(c,"La hora debe ser futura",Toast.LENGTH_SHORT).show();return@TimePickerDialog}
                save(c,r,note,at)
            },now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true)
            time.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            time.show()
        },now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH))
        date.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        date.show()
    }

    private fun save(c:Context,r:CallRecord,n:String,at:Long?){val rr=r.copy(note=n,reminderAt=at,status=if(at==null)"DONE" else "PENDING");val id=CallDb(c).insert(rr);if(at!=null)ReminderScheduler.schedule(c,id,rr);remove()}
    private fun remove(){try{view?.let{wm?.removeView(it)}}catch(_:Exception){};view=null}
    private fun fmt(m:Int)=when(m){60->"1 hora";120->"2 horas";240->"4 horas";else->"$m min"}
    private fun dp(c:Context,n:Int)=(n*c.resources.displayMetrics.density).toInt()
    private fun t(c:Context,s:String,z:Float,col:Int,b:Boolean=false)=TextView(c).apply{text=s;textSize=z;setTextColor(col);setTypeface(null,if(b)1 else 0);setPadding(0,dp(c,3),0,0)}
    private fun bg(c:Int,r:Int,s:Int)=GradientDrawable().apply{setColor(c);cornerRadius=r.toFloat();setStroke(1,s)}
}

object ReminderScheduler {
    fun schedule(c: Context, id: Long, r: CallRecord) {
        val at = r.reminderAt ?: return
        val intent = Intent(c, ReminderReceiver::class.java).apply { putExtra("id",id); putExtra("name",r.name); putExtra("number",r.number); putExtra("note",r.note) }
        val p = PendingIntent.getBroadcast(c,(id % Int.MAX_VALUE).toInt(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = c.getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
            } else if (Build.VERSION.SDK_INT >= 23) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
            } else alarm.setExact(AlarmManager.RTC_WAKEUP,at,p)
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
        }
    }

    fun cancel(c: Context, id: Long) {
        val p=PendingIntent.getBroadcast(c,(id%Int.MAX_VALUE).toInt(),Intent(c,ReminderReceiver::class.java),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if(p!=null)c.getSystemService(AlarmManager::class.java).cancel(p)
    }
}

class ReminderReceiver:BroadcastReceiver(){
    override fun onReceive(c:Context,i:Intent){
        val id=i.getLongExtra("id",0L); if(id>0) CallDb(c).markReminderFired(id)
        val ch="ringback_reminders";val n=c.getSystemService(NotificationManager::class.java);if(Build.VERSION.SDK_INT>=26)n.createNotificationChannel(NotificationChannel(ch,"Recordatorios RingBack",NotificationManager.IMPORTANCE_HIGH))
        val name=i.getStringExtra("name")?:"Contacto";val num=i.getStringExtra("number")?:"";val note=i.getStringExtra("note").orEmpty()
        val p=PendingIntent.getActivity(c,num.hashCode(),Intent(Intent.ACTION_DIAL,Uri.parse("tel:${Uri.encode(num)}")),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val x=Notification.Builder(c,ch).setSmallIcon(android.R.drawable.ic_menu_call).setContentTitle("Devolver llamada a $name").setContentText(if(note.isBlank())num else note).setAutoCancel(true).setContentIntent(p).setPriority(Notification.PRIORITY_HIGH).build()
        if(Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==0)n.notify((id and 0x7fffffff).toInt(),x)
    }
}

class BootReceiver:BroadcastReceiver(){
    override fun onReceive(c:Context,i:Intent){
        val action=i.action
        if(action!=Intent.ACTION_BOOT_COMPLETED && action!=Intent.ACTION_MY_PACKAGE_REPLACED && action!=AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        if(action==AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED && Build.VERSION.SDK_INT>=31 && !c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) return
        CallDb(c).pending().filter{it.reminderAt!=null && it.reminderAt>System.currentTimeMillis()}.forEach{ReminderScheduler.schedule(c,it.id,it)}
    }
}
