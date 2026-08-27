package com.lc.schedule

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name     = intent.getStringExtra("course_name") ?: return
        val location = intent.getStringExtra("location") ?: ""
        val time     = intent.getStringExtra("start_time") ?: ""
        val index    = intent.getIntExtra("msg_index", 0)

        val bodies = listOf(
            "小乖，$name 还有8分钟，去 $location 吧",
            "宝宝快准备，$name 快开始了，在 $location",
            "该动了，$name $time 在 $location 等你",
            "去上课，$name，$location，别迟到",
            "小猫，$name 快开始了，$location 见"
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("schedule", "课程提醒", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.notify(name.hashCode(),
            NotificationCompat.Builder(context, "schedule")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("上课提醒")
                .setContentText(bodies[index % bodies.size])
                .setAutoCancel(true)
                .build()
        )
        ScheduleWidget.updateAll(context)
    }
}

class DailyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.scheduleDaily(context)
    }
}

object NotificationHelper {

    fun scheduleDaily(context: Context) {
        scheduleTodayNotifications(context)
        scheduleNextDayWakeup(context)
    }

    fun scheduleTodayNotifications(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nowMins = Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }

        ScheduleData.getTodayCourses().forEachIndexed { idx, course ->
            val (sh, sm) = course.getStartTime()
            val notifyMins = sh * 60 + sm - 8
            if (notifyMins <= nowMins) return@forEachIndexed

            val trigger = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, notifyMins / 60)
                set(Calendar.MINUTE, notifyMins % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val pi = PendingIntent.getBroadcast(
                context,
                course.dayOfWeek * 100 + course.startLesson,
                Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("course_name", course.name)
                    putExtra("location", course.location)
                    putExtra("start_time", ScheduleData.formatTime(sh, sm))
                    putExtra("msg_index", idx)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pi)
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pi)
            }
        }
    }

    private fun scheduleNextDayWakeup(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
        }
        am.set(AlarmManager.RTC_WAKEUP, trigger.timeInMillis,
            PendingIntent.getBroadcast(context, 8888,
                Intent(context, DailyReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
    }
}
