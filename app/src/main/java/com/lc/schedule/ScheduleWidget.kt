package com.lc.schedule

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class ScheduleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ScheduleWidget::class.java))
            ids.forEach { updateWidget(context, mgr, it) }
        }

        fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val week = ScheduleData.getCurrentWeek()
            val cal = Calendar.getInstance()
            val days = arrayOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")

            val headerText = when {
                week <= 0  -> "开学前"
                week > 18  -> "学期结束"
                else       -> "第${week}周 ${days[cal.get(Calendar.DAY_OF_WEEK)]}"
            }
            views.setTextViewText(R.id.tv_week, headerText)

            val current = ScheduleData.getCurrentCourse()
            val next    = ScheduleData.getNextCourse()
            when {
                current != null -> {
                    val (eh, em) = current.getEndTime()
                    views.setTextViewText(R.id.tv_course_name, "▶ ${current.name}")
                    views.setTextViewText(R.id.tv_course_detail,
                        "${current.location}  ${ScheduleData.formatTime(eh, em)}下课")
                }
                next != null -> {
                    val (sh, sm) = next.getStartTime()
                    views.setTextViewText(R.id.tv_course_name, next.name)
                    views.setTextViewText(R.id.tv_course_detail,
                        "${next.location}  ${ScheduleData.formatTime(sh, sm)}上课")
                }
                else -> {
                    views.setTextViewText(R.id.tv_course_name, "今天没课了")
                    views.setTextViewText(R.id.tv_course_detail, "好好休息")
                }
            }

            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(id, views)
        }
    }
}
