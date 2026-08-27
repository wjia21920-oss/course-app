package com.lc.schedule

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val courseColors = listOf(
        "#B8C9E0", "#C8B8D8", "#B8D4C8", "#D4C8A8",
        "#D4B8B8", "#C8C0D8", "#B8D0C8", "#C8D4B8"
    )

    private var displayWeek = 0
    private lateinit var weekLabel: TextView
    private lateinit var gridContainer: LinearLayout
    private lateinit var todayContent: LinearLayout
    private lateinit var noteInput: EditText
    private lateinit var tabViews: List<TextView>
    private lateinit var currentBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        NotificationHelper.scheduleDaily(this)
        ScheduleWidget.updateAll(this)

        displayWeek = ScheduleData.getCurrentWeek().coerceAtLeast(1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FAFAFA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        }
        setContentView(root)

        root.addView(buildTopBar())

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val tabNames = listOf("日程", "课表", "备忘录")
        tabViews = tabNames.mapIndexed { i, name ->
            TextView(this).apply {
                text = name
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { switchTab(i) }
            }
        }
        tabViews.forEach { tabBar.addView(it) }
        root.addView(tabBar)

        root.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val contentWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        todayContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(80))
        }

        gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(80))
            visibility = View.GONE
        }

        val noteWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(80))
            visibility = View.GONE
        }
        noteInput = EditText(this).apply {
            hint = "记点什么……"
            textSize = 14f
            setTextColor(Color.parseColor("#222222"))
            setHintTextColor(Color.parseColor("#BBBBBB"))
            background = null
            minLines = 12
            gravity = Gravity.TOP
        }
        noteWrap.addView(noteInput)

        contentWrap.addView(todayContent)
        contentWrap.addView(gridContainer)
        contentWrap.addView(noteWrap)
        scroll.addView(contentWrap)
        root.addView(scroll)

        currentBar = buildCurrentBar()
        root.addView(currentBar)

        updateTabStyles(0)
        buildTodayPage()
        buildGridPage()
        updateCurrentBar()
    }

    private fun buildTopBar(): LinearLayout {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val days = arrayOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val todayStr = days[cal.get(Calendar.DAY_OF_WEEK)]

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(48), dp(20), dp(16))
            gravity = Gravity.CENTER_VERTICAL
        }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(TextView(this).apply {
            text = "Hi，${month}月${day}日 $todayStr"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111111"))
        })
        val week = ScheduleData.getCurrentWeek()
        left.addView(TextView(this).apply {
            text = when {
                week <= 0 -> "还没开学"
                week > 18 -> "学期结束"
                else      -> "第${week}周 · 今日${ScheduleData.getTodayCourses().size}门课"
            }
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            setPadding(0, dp(4), 0, 0)
        })
        bar.addView(left)
        return bar
    }

    private fun buildCurrentBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(12), dp(20), dp(24))
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            elevation = dp(8).toFloat()
        }
    }

    private fun updateCurrentBar() {
        currentBar.removeAllViews()
        val current = ScheduleData.getCurrentCourse()
        val next    = ScheduleData.getNextCourse()
        val course  = current ?: next ?: return

        currentBar.addView(View(this).apply {
            val size = dp(8)
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginEnd = dp(10)
                it.gravity = Gravity.CENTER_VERTICAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (current != null) Color.parseColor("#4CAF89")
                         else Color.parseColor("#AAAAAA"))
            }
        })
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(this).apply {
            text = course.name
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111111"))
        })
        textCol.addView(TextView(this).apply {
            val (sh, sm) = course.getStartTime()
            val (eh, em) = course.getEndTime()
            text = "${course.location}  ${ScheduleData.formatTime(sh,sm)}–${ScheduleData.formatTime(eh,em)}"
            textSize = 11f
            setTextColor(Color.parseColor("#999999"))
        })
        currentBar.addView(textCol)
        currentBar.addView(TextView(this).apply {
            text = if (current != null) "进行中" else "即将开始"
            textSize = 12f
            setTextColor(if (current != null) Color.parseColor("#4CAF89")
                         else Color.parseColor("#AAAAAA"))
        })
    }

    private fun switchTab(index: Int) {
        updateTabStyles(index)
        todayContent.visibility  = if (index == 0) View.VISIBLE else View.GONE
        gridContainer.visibility = if (index == 1) View.VISIBLE else View.GONE
        val noteWrap = (todayContent.parent as LinearLayout).getChildAt(2)
        noteWrap.visibility = if (index == 2) View.VISIBLE else View.GONE
    }

    private fun updateTabStyles(selected: Int) {
        tabViews.forEachIndexed { i, tv ->
            tv.setTextColor(if (i == selected) Color.parseColor("#111111")
                            else Color.parseColor("#BBBBBB"))
            tv.typeface = if (i == selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun buildTodayPage() {
        todayContent.removeAllViews()
        val courses = ScheduleData.getTodayCourses()
        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        if (courses.isEmpty()) {
            todayContent.addView(TextView(this).apply {
                text = "今天没有课"
                textSize = 16f
                setTextColor(Color.parseColor("#BBBBBB"))
                gravity = Gravity.CENTER
                setPadding(0, dp(60), 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            return
        }

        val totalMins = courses.sumOf { it.getEndMinutes() - it.getStartMinutes() }
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(20) }
        }
        listOf(
            Triple("${courses.size}", "门课", "#5B6FD8"),
            Triple(formatHours(totalMins), "上课", "#4CAF89"),
            Triple(formatHours(
                (courses.last().getEndMinutes() - courses.first().getStartMinutes()) - totalMins
            ), "空闲", "#D4A853")
        ).forEachIndexed { i, (val1, label, color) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(14), dp(12), dp(14))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(14).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (i < 2) it.marginEnd = dp(10)
                }
            }
            card.addView(TextView(this).apply {
                text = val1; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(color)); gravity = Gravity.CENTER
            })
            card.addView(TextView(this).apply {
                text = label; textSize = 10f
                setTextColor(Color.parseColor("#BBBBBB")); gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            })
            statsRow.addView(card)
        }
        todayContent.addView(statsRow)

        courses.forEachIndexed { idx, course ->
            val (sh, sm) = course.getStartTime()
            val (eh, em) = course.getEndTime()
            val startMins = sh * 60 + sm
            val endMins   = eh * 60 + em
            val isCurrent = currentMins in startMins..endMins
            val isPast    = currentMins > endMins

            if (idx > 0) {
                val gap = startMins - courses[idx-1].getEndMinutes()
                if (gap > 0) {
                    val h = gap / 60; val m = gap % 60
                    todayContent.addView(TextView(this).apply {
                        text = "  空闲 ${if (h>0) "${h}h" else ""}${if (m>0) "${m}min" else ""}"
                        textSize = 10f
                        setTextColor(Color.parseColor("#CCCCCC"))
                        setPadding(dp(88), dp(4), 0, dp(4))
                    })
                }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(8) }
            }

            val timeCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.END
                layoutParams = LinearLayout.LayoutParams(dp(72),
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, dp(2), dp(12), 0)
            }
            timeCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(sh, sm); textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#333333"))
                gravity = Gravity.END
            })
            timeCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(eh, em); textSize = 10f
                setTextColor(Color.parseColor("#CCCCCC"))
                gravity = Gravity.END; setPadding(0, dp(2), 0, 0)
            })

            val dot = View(this).apply {
                val sz = if (isCurrent) dp(10) else dp(8)
                layoutParams = LinearLayout.LayoutParams(sz, sz).also {
                    it.topMargin = dp(4); it.marginEnd = dp(10)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(when {
                        isPast    -> Color.parseColor("#E0E0E0")
                        isCurrent -> Color.parseColor("#5B6FD8")
                        else      -> Color.parseColor(courseColors[idx % courseColors.size])
                    })
                }
            }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#F5F5F5") else Color.WHITE)
                    cornerRadius = dp(12).toFloat()
                    if (!isPast) setStroke(dp(1), Color.parseColor(
                        courseColors[idx % courseColors.size] + "66"))
                }
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val inner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            inner.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(3),
                    LinearLayout.LayoutParams.MATCH_PARENT).also { it.marginEnd = dp(10) }
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#E0E0E0")
                             else Color.parseColor(courseColors[idx % courseColors.size]))
                    cornerRadius = dp(2).toFloat()
                }
            })
            val textCol2 = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol2.addView(TextView(this).apply {
                text = course.name; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#BBBBBB") else Color.parseColor("#222222"))
            })
            textCol2.addView(TextView(this).apply {
                text = "${course.location}  ${course.teacher}"
                textSize = 11f; setTextColor(Color.parseColor("#AAAAAA"))
                setPadding(0, dp(3), 0, 0)
            })
            if (isCurrent) {
                textCol2.addView(TextView(this).apply {
                    text = "▶ 进行中"; textSize = 10f
                    setTextColor(Color.parseColor("#5B6FD8")); setPadding(0, dp(4), 0, 0)
                })
            }
            inner.addView(textCol2)
            card.addView(inner)
            row.addView(timeCol); row.addView(dot); row.addView(card)
            todayContent.addView(row)
        }
    }

    private fun buildGridPage() {
        gridContainer.removeAllViews()

        val weekBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        weekLabel = TextView(this).apply {
            text = "第${displayWeek}周"
            textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#222222")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        weekBar.addView(TextView(this).apply {
            text = "＜"; textSize = 16f; setTextColor(Color.parseColor("#999999"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { if (displayWeek > 1) { displayWeek--; refreshGrid() } }
        })
        weekBar.addView(weekLabel)
        weekBar.addView(TextView(this).apply {
            text = "＞"; textSize = 16f; setTextColor(Color.parseColor("#999999"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { if (displayWeek < 18) { displayWeek++; refreshGrid() } }
        })
        weekBar.addView(TextView(this).apply {
            text = "本周"; textSize = 11f; setTextColor(Color.parseColor("#5B6FD8"))
            setPadding(dp(10), dp(4), dp(4), dp(4))
            setOnClickListener {
                displayWeek = ScheduleData.getCurrentWeek().coerceAtLeast(1); refreshGrid()
            }
        })
        gridContainer.addView(weekBar)

        gridContainer.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        })

        val tableScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        tableScroll.addView(buildWeekGrid())
        gridContainer.addView(tableScroll)
    }

    private fun refreshGrid() {
        weekLabel.text = "第${displayWeek}周"
        val tableScroll = (gridContainer.getChildAt(2) as? HorizontalScrollView) ?: return
        tableScroll.removeAllViews()
        tableScroll.addView(buildWeekGrid())
    }

    // 计算某周周一~周五的 (月, 日)
    private fun getWeekDates(week: Int): List<Pair<Int, Int>> {
        val semStart = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        semStart.add(Calendar.DAY_OF_YEAR, (week - 1) * 7)
        return (0..4).map { d ->
            val c = semStart.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, d)
            Pair(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }
    }

    // 构建单个课程块（长方形）
    private fun buildCourseBlock(course: Course, isActive: Boolean, slotH: Int): LinearLayout {
        val span    = course.endLesson - course.startLesson + 1
        val height  = slotH * span - dp(2)
        val idx     = ScheduleData.COURSES.indexOf(course).coerceAtLeast(0)
        val hex     = courseColors[idx % courseColors.size]
        val bgColor = if (isActive) Color.parseColor(hex + "BB")
                      else          Color.parseColor(hex + "2A")
        val textMain = if (isActive) "#333333" else "#BBBBBB"
        val textSub  = if (isActive) "#666666" else "#CCCCCC"

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(6).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height).also {
                it.bottomMargin = dp(2)
            }
            if (!isActive) {
                addView(TextView(this@MainActivity).apply {
                    text = "非本周"
                    textSize = 7f
                    setTextColor(Color.parseColor("#BBBBBB"))
                })
            }
            addView(TextView(this@MainActivity).apply {
                text = course.name
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(textMain))
                maxLines = when {
                    span >= 4 -> 5
                    span == 3 -> 4
                    span == 2 -> 2
                    else      -> 1
                }
            })
            addView(TextView(this@MainActivity).apply {
                text = "@${course.location}"
                textSize = 7f
                setTextColor(Color.parseColor(textSub))
                setPadding(0, dp(2), 0, 0)
            })
        }
    }

    // 新版周视图：左侧节次+时间，顶部日期，长方形课程块
    private fun buildWeekGrid(): LinearLayout {
        val dayNames  = listOf("一", "二", "三", "四", "五")
        val dayCals   = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                               Calendar.THURSDAY, Calendar.FRIDAY)
        val currentWeek = ScheduleData.getCurrentWeek()
        val currentDay  = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val weekDates   = getWeekDates(displayWeek)
        val slotH       = dp(52)
        val headerH     = dp(52)
        val leftW       = dp(48)
        val colW        = dp(64)

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // 左侧：节次编号 + 起止时间
        val lessonCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(leftW, LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.WHITE)
        }
        val (mo0, _) = weekDates[0]
        lessonCol.addView(TextView(this).apply {
            text = "${mo0}月"
            textSize = 10f
            setTextColor(Color.parseColor("#BBBBBB"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(leftW, headerH)
        })
        for (i in 1..12) {
            val times = LESSON_TIMES[i] ?: continue
            val (sh, sm) = times.first
            val (eh, em) = times.second
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(leftW, slotH)
            }
            cell.addView(TextView(this).apply {
                text = "$i"; textSize = 11f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER
            })
            cell.addView(TextView(this).apply {
                text = ScheduleData.formatTime(sh, sm); textSize = 8f
                setTextColor(Color.parseColor("#CCCCCC")); gravity = Gravity.CENTER
                setPadding(0, dp(1), 0, 0)
            })
            cell.addView(TextView(this).apply {
                text = ScheduleData.formatTime(eh, em); textSize = 7f
                setTextColor(Color.parseColor("#DDDDDD")); gravity = Gravity.CENTER
            })
            lessonCol.addView(cell)
        }
        table.addView(lessonCol)

        // 每天一列
        dayCals.forEachIndexed { di, dayCal ->
            val (mo, dy) = weekDates[di]
            val isToday = displayWeek == currentWeek && dayCal == currentDay

            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(colW, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(dp(2), 0, dp(2), 0)
            }

            // 表头：周几 + 日期（今日加蓝圈）
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, headerH)
            }
            header.addView(TextView(this).apply {
                text = "周${dayNames[di]}"; textSize = 10f; gravity = Gravity.CENTER
                setTextColor(if (isToday) Color.parseColor("#5B6FD8")
                             else         Color.parseColor("#AAAAAA"))
            })
            val frame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).also {
                    it.topMargin = dp(2)
                    it.gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            if (isToday) {
                frame.addView(View(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(28), dp(28)).also {
                        it.gravity = Gravity.CENTER
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#5B6FD8"))
                    }
                })
            }
            frame.addView(TextView(this).apply {
                text = "$dy"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isToday) Color.WHITE else Color.parseColor("#333333"))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
            })
            header.addView(frame)
            col.addView(header)

            // 课程：本周正常色；非本周透明降色
            val activeCourses = ScheduleData.getCoursesForDay(displayWeek, dayCal)
            val allCoursesDay = ScheduleData.COURSES
                .filter { it.dayOfWeek == dayCal }
                .sortedBy { it.startLesson }

            // 先标记被本周课程占用的节次
            val occupied = mutableSetOf<Int>()
            for (c in activeCourses) {
                for (l in c.startLesson..c.endLesson) occupied.add(l)
            }

            var lesson = 1
            while (lesson <= 12) {
                // 本周有课 → 正常渲染
                val active = activeCourses.firstOrNull { it.startLesson == lesson }
                if (active != null) {
                    col.addView(buildCourseBlock(active, true, slotH))
                    lesson = active.endLesson + 1
                    continue
                }
                if (lesson in occupied) { lesson++; continue }

                // 非本周有课（且不与本周课重叠）→ 半透明
                val ghost = allCoursesDay
                    .filter { !it.isActiveInWeek(displayWeek) && it.startLesson == lesson }
                    .filter { (it.startLesson..it.endLesson).none { l -> l in occupied } }
                    .firstOrNull()
                if (ghost != null) {
                    col.addView(buildCourseBlock(ghost, false, slotH))
                    lesson = ghost.endLesson + 1
                    continue
                }

                // 空格
                col.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, slotH)
                })
                lesson++
            }

            table.addView(col)
        }
        return table
    }

    private fun formatHours(mins: Int): String {
        val m = maxOf(mins, 0)
        val h = m / 60; val min = m % 60
        return if (h > 0 && min > 0) "${h}.${min * 10 / 60}h"
        else if (h > 0) "${h}h" else "${min}m"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
