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

    private val colorSchemes = listOf(
        arrayOf("#7890C8", "#C8D4EC", "#ECF0F8", "#4A6098"),
        arrayOf("#D06888", "#F0C0CC", "#FCE8EE", "#B04868"),
        arrayOf("#68B080", "#B8D8C0", "#E4F4EC", "#3A8050"),
        arrayOf("#C8A048", "#ECD8A0", "#F8F0D8", "#A87828"),
        arrayOf("#8870C0", "#C8C0E8", "#ECEAF8", "#6050A0"),
        arrayOf("#50A8C0", "#A8D8E8", "#DCF0F8", "#3080A0"),
        arrayOf("#C07050", "#E8C0A8", "#F8E8DC", "#A05030"),
        arrayOf("#70A870", "#B8D8B8", "#E4F4E4", "#408040")
    )

    private var displayWeek = 0
    private lateinit var weekLabel: TextView
    private lateinit var gridContainer: LinearLayout
    private lateinit var scheduleSection: LinearLayout
    private lateinit var todayContent: LinearLayout
    private lateinit var tomorrowContent: LinearLayout
    private lateinit var noteContent: LinearLayout
    private lateinit var noteInput: EditText
    private lateinit var mainTabViews: List<LinearLayout>
    private lateinit var subTabViews: List<TextView>

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
            setBackgroundColor(Color.parseColor("#F0F1F8"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        }
        setContentView(root)

        root.addView(buildTopDateCard())

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        val contentWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        scheduleSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scheduleSection.addView(buildSubTabBar())

        todayContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        tomorrowContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            visibility = View.GONE
        }
        noteContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            visibility = View.GONE
        }
        noteInput = EditText(this).apply {
            hint = "记点什么……"
            textSize = 14f
            setTextColor(Color.parseColor("#222222"))
            setHintTextColor(Color.parseColor("#BBBBBB"))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            minLines = 8
            gravity = Gravity.TOP
        }
        noteContent.addView(noteInput)

        scheduleSection.addView(todayContent)
        scheduleSection.addView(tomorrowContent)
        scheduleSection.addView(noteContent)

        gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        contentWrap.addView(scheduleSection)
        contentWrap.addView(gridContainer)
        scroll.addView(contentWrap)
        root.addView(scroll)
        root.addView(buildBottomNav())

        updateMainTabStyles(0)
        updateSubTabStyles(0)
        buildTodayPage()
        buildTomorrowPage()
        buildGridPage()
    }

    private fun buildTopDateCard(): LinearLayout {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val days = arrayOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val dayStr = days[cal.get(Calendar.DAY_OF_WEEK)]
        val todayCount = ScheduleData.getTodayCourses().size

        val tomorrowCal = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, 1) }
        val week = ScheduleData.getCurrentWeek()
        val tomorrowCount = if (week in 1..18)
            ScheduleData.getCoursesForDay(week, tomorrowCal.get(Calendar.DAY_OF_WEEK)).size else 0

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(44), dp(20), dp(16))
            gravity = Gravity.CENTER_VERTICAL

            val left = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val iconRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            iconRow.addView(TextView(this@MainActivity).apply {
                text = "✦  今日日程"
                textSize = 12f
                setTextColor(Color.parseColor("#AAAAAA"))
            })
            left.addView(iconRow)
            left.addView(TextView(this@MainActivity).apply {
                text = "${month}月${day}日"
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#111111"))
                setPadding(0, dp(4), 0, 0)
            })
            left.addView(TextView(this@MainActivity).apply {
                text = "$dayStr  ·  $todayCount 门课"
                textSize = 13f
                setTextColor(Color.parseColor("#999999"))
                setPadding(0, dp(4), 0, 0)
            })
            addView(left)

            val rightCard = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#ECEEF8"))
                    cornerRadius = dp(14).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dp(70),
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            rightCard.addView(TextView(this@MainActivity).apply {
                text = "明日"; textSize = 11f
                setTextColor(Color.parseColor("#8890D8")); gravity = Gravity.CENTER
            })
            rightCard.addView(TextView(this@MainActivity).apply {
                text = "$tomorrowCount"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#8890D8")); gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            })
            rightCard.addView(TextView(this@MainActivity).apply {
                text = "门课"; textSize = 11f
                setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER
            })
            addView(rightCard)
        }
    }

    private fun buildSubTabBar(): LinearLayout {
        val bg = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E8E9F4"))
                cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(4) }
        }
        val names = listOf("今日课程", "明日课程", "备忘录")
        subTabViews = names.mapIndexed { i, name ->
            TextView(this).apply {
                text = name; textSize = 13f; gravity = Gravity.CENTER
                setPadding(0, dp(9), 0, dp(9))
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { switchSubTab(i) }
            }
        }
        subTabViews.forEach { bg.addView(it) }
        return bg
    }

    private fun buildBottomNav(): LinearLayout {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            elevation = dp(8).toFloat()
        }
        wrap.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#EEEEEE"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        })
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(24))
        }
        val items = listOf("日程" to "📅", "课表" to "📋")
        mainTabViews = items.mapIndexed { i, (label, icon) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(4), dp(6), dp(4), dp(6))
                setOnClickListener { switchMainTab(i) }
            }
            item.addView(TextView(this).apply {
                text = icon; textSize = 20f; gravity = Gravity.CENTER
            })
            item.addView(TextView(this).apply {
                text = label; textSize = 11f; gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            })
            item
        }
        mainTabViews.forEach { bar.addView(it) }
        wrap.addView(bar)
        return wrap
    }

    private fun switchMainTab(index: Int) {
        updateMainTabStyles(index)
        scheduleSection.visibility = if (index == 0) View.VISIBLE else View.GONE
        gridContainer.visibility   = if (index == 1) View.VISIBLE else View.GONE
    }

    private fun updateMainTabStyles(selected: Int) {
        mainTabViews.forEachIndexed { i, item ->
            val color = if (i == selected) "#333333" else "#BBBBBB"
            (item.getChildAt(0) as? TextView)?.setTextColor(Color.parseColor(color))
            (item.getChildAt(1) as? TextView)?.setTextColor(Color.parseColor(color))
            (item.getChildAt(1) as? TextView)?.typeface =
                if (i == selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun switchSubTab(index: Int) {
        updateSubTabStyles(index)
        todayContent.visibility    = if (index == 0) View.VISIBLE else View.GONE
        tomorrowContent.visibility = if (index == 1) View.VISIBLE else View.GONE
        noteContent.visibility     = if (index == 2) View.VISIBLE else View.GONE
    }

    private fun updateSubTabStyles(selected: Int) {
        subTabViews.forEachIndexed { i, tv ->
            if (i == selected) {
                tv.setTextColor(Color.parseColor("#222222"))
                tv.typeface = Typeface.DEFAULT_BOLD
                tv.background = GradientDrawable().apply {
                    setColor(Color.WHITE); cornerRadius = dp(12).toFloat()
                }
            } else {
                tv.setTextColor(Color.parseColor("#AAAAAA"))
                tv.typeface = Typeface.DEFAULT
                tv.background = null
            }
        }
    }

    private fun buildTodayPage() {
        todayContent.removeAllViews()
        buildScheduleContent(todayContent, ScheduleData.getTodayCourses(), isToday = true)
    }

    private fun buildTomorrowPage() {
        tomorrowContent.removeAllViews()
        val week = ScheduleData.getCurrentWeek()
        val tomorrowCal = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, 1) }
        val courses = if (week in 1..18)
            ScheduleData.getCoursesForDay(week, tomorrowCal.get(Calendar.DAY_OF_WEEK))
        else emptyList()
        buildScheduleContent(tomorrowContent, courses, isToday = false)
    }

    private fun buildScheduleContent(container: LinearLayout, courses: List<Course>, isToday: Boolean) {
        val cal = Calendar.getInstance()
        val currentMins = if (isToday)
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE) else -1

        val timelineCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(20).toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(10) }
        }

        timelineCard.addView(TextView(this).apply {
            text = if (isToday) "今日日程" else "明日日程"
            textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#222222"))
        })
        timelineCard.addView(TextView(this).apply {
            text = "按上课时间顺序展示课程安排"
            textSize = 11f; setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, dp(3), 0, dp(12))
        })

        if (courses.isEmpty()) {
            timelineCard.addView(TextView(this).apply {
                text = if (isToday) "今日无课，好好休息 ☀" else "明日无课"
                textSize = 14f; setTextColor(Color.parseColor("#CCCCCC"))
                gravity = Gravity.CENTER
                setPadding(0, dp(24), 0, dp(24))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            container.addView(timelineCard)
            return
        }

        courses.forEachIndexed { idx, course ->
            val (sh, sm) = course.getStartTime()
            val (eh, em) = course.getEndTime()
            val isCurrent = currentMins in course.getStartMinutes()..course.getEndMinutes()
            val isPast    = currentMins > course.getEndMinutes()
            val scheme    = colorSchemes[idx % colorSchemes.size]

            if (idx > 0) {
                val gap = course.getStartMinutes() - courses[idx-1].getEndMinutes()
                if (gap > 0) {
                    val h = gap / 60; val m = gap % 60
                    timelineCard.addView(TextView(this).apply {
                        text = "  空闲  ${if (h>0) "${h}h " else ""}${if (m>0) "${m}min" else ""}"
                        textSize = 11f; setTextColor(Color.parseColor("#BBBBBB"))
                        setPadding(dp(60), dp(6), 0, dp(6))
                    })
                }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(4) }
            }

            // 左侧时间+节次
            val leftCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(dp(68),
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, dp(4), dp(8), 0)
            }
            val timeRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            }
            timeRow.addView(TextView(this).apply {
                text = ScheduleData.formatTime(sh, sm); textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#222222"))
            })
            if (isCurrent) {
                timeRow.addView(View(this).apply {
                    val sz = dp(7)
                    layoutParams = LinearLayout.LayoutParams(sz, sz).also {
                        it.marginStart = dp(4); it.gravity = Gravity.CENTER_VERTICAL
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(scheme[0]))
                    }
                })
            }
            leftCol.addView(timeRow)
            leftCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(eh, em); textSize = 11f
                setTextColor(Color.parseColor("#BBBBBB")); setPadding(0, dp(1), 0, dp(8))
            })
            val lessonLabel = "第${course.startLesson}${if (course.endLesson > course.startLesson) "-${course.endLesson}" else ""}节"
            leftCol.addView(TextView(this).apply {
                text = lessonLabel; textSize = 10f; gravity = Gravity.CENTER
                setPadding(dp(6), dp(4), dp(6), dp(4))
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#F0F0F0") else Color.parseColor(scheme[1]))
                    cornerRadius = dp(10).toFloat()
                }
                setTextColor(if (isPast) Color.parseColor("#BBBBBB") else Color.parseColor(scheme[3]))
            })
            row.addView(leftCol)

            // 右侧渐变课程卡
            val span = course.endLesson - course.startLesson + 1
            val cardMinH = dp(70) + (span - 1) * dp(20)
            val courseCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = if (isPast) {
                    GradientDrawable().apply {
                        setColor(Color.parseColor("#F5F5F5"))
                        cornerRadius = dp(14).toFloat()
                    }
                } else {
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(Color.parseColor(scheme[1]), Color.parseColor(scheme[2]))
                    ).apply { cornerRadius = dp(14).toFloat() }
                }
                // 修复：minimumHeight 在 apply 块里直接设置，不放在 layoutParams
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                minimumHeight = cardMinH
                setPadding(dp(12), dp(12), dp(12), dp(12))
                gravity = Gravity.CENTER_VERTICAL
            }

            courseCard.addView(View(this).apply {
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#DDDDDD") else Color.parseColor(scheme[0]))
                    cornerRadius = dp(3).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dp(4),
                    LinearLayout.LayoutParams.MATCH_PARENT).also { it.marginEnd = dp(10) }
                minimumHeight = dp(40)
            })

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            if (isCurrent) {
                textCol.addView(TextView(this).apply {
                    text = "▶ 进行中"; textSize = 9f
                    setTextColor(Color.parseColor(scheme[3])); setPadding(0, 0, 0, dp(3))
                })
            }
            textCol.addView(TextView(this).apply {
                text = course.name; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#BBBBBB") else Color.parseColor(scheme[3]))
            })
            textCol.addView(TextView(this).apply {
                text = "@${course.location}"; textSize = 11f
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor(scheme[0]))
                setPadding(0, dp(4), 0, 0)
            })
            courseCard.addView(textCol)
            row.addView(courseCard)
            timelineCard.addView(row)
        }
        container.addView(timelineCard)

        // 统计三卡
        if (courses.isNotEmpty()) {
            val totalMins = courses.sumOf { it.getEndMinutes() - it.getStartMinutes() }
            val freeMins  = maxOf(0,
                courses.last().getEndMinutes() - courses.first().getStartMinutes() - totalMins)
            val statsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            // 修复：用 arrayOf 替代 Triple，避免4参数报错
            listOf(
                arrayOf("${courses.size}门", "今日门数",  "#7890D8", "#EEF0FC"),
                arrayOf(formatHours(totalMins), "上课时长", "#68C090", "#EAF6EE"),
                arrayOf(formatHours(freeMins),  "空闲时间", "#E8A040", "#FEF6E4")
            ).forEachIndexed { i, item ->
                val val1 = item[0]; val label = item[1]
                val textColor = item[2]; val bgColor = item[3]
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(bgColor)); cornerRadius = dp(16).toFloat()
                    }
                    setPadding(dp(8), dp(14), dp(8), dp(14))
                    layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                        if (i < 2) it.marginEnd = dp(8)
                    }
                }
                card.addView(TextView(this).apply {
                    text = val1; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor(textColor)); gravity = Gravity.CENTER
                })
                card.addView(TextView(this).apply {
                    text = label; textSize = 10f
                    setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER
                    setPadding(0, dp(3), 0, 0)
                })
                statsRow.addView(card)
            }
            container.addView(statsRow)
        }
    }

    private fun buildGridPage() {
        gridContainer.removeAllViews()
        val weekBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE); setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        weekLabel = TextView(this).apply {
            text = "第${displayWeek}周"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#222222")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        weekBar.addView(TextView(this).apply {
            text = "＜"; textSize = 18f; setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(dp(4), dp(4), dp(12), dp(4))
            setOnClickListener { if (displayWeek > 1) { displayWeek--; refreshGrid() } }
        })
        weekBar.addView(weekLabel)
        weekBar.addView(TextView(this).apply {
            text = "＞"; textSize = 18f; setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(dp(12), dp(4), dp(4), dp(4))
            setOnClickListener { if (displayWeek < 18) { displayWeek++; refreshGrid() } }
        })
        weekBar.addView(TextView(this).apply {
            text = "本周"; textSize = 11f; setTextColor(Color.parseColor("#7890D8"))
            setPadding(dp(12), dp(4), dp(4), dp(4))
            setOnClickListener {
                displayWeek = ScheduleData.getCurrentWeek().coerceAtLeast(1); refreshGrid()
            }
        })
        gridContainer.addView(weekBar)
        gridContainer.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#EEEEEE"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        })
        val tableScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            isHorizontalScrollBarEnabled = false
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

    private fun getWeekDates(week: Int): List<Pair<Int, Int>> {
        val semStart = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 31, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        semStart.add(Calendar.DAY_OF_YEAR, (week - 1) * 7)
        return (0..4).map { d ->
            val c = semStart.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, d)
            Pair(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }
    }

    private fun buildWeekGrid(): LinearLayout {
        val dayNames = listOf("一", "二", "三", "四", "五")
        val dayCals  = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                              Calendar.THURSDAY, Calendar.FRIDAY)
        val currentWeek = ScheduleData.getCurrentWeek()
        val currentDay  = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val weekDates   = getWeekDates(displayWeek)
        val slotH = dp(56); val headerH = dp(50)
        val leftW = dp(40); val colW = dp(58)

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.parseColor("#F0F1F8"))
        }

        val lessonCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(leftW, LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.WHITE)
        }
        lessonCol.addView(TextView(this).apply {
            text = "${weekDates[0].first}月"; textSize = 9f
            setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(leftW, headerH)
        })
        for (i in 1..12) {
            val times = LESSON_TIMES[i] ?: continue
            val (sh, sm) = times.first; val (eh, em) = times.second
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(leftW, slotH)
                setBackgroundColor(Color.WHITE)
            }
            cell.addView(TextView(this).apply {
                text = "$i"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#888888")); gravity = Gravity.CENTER
            })
            cell.addView(TextView(this).apply {
                text = ScheduleData.formatTime(sh, sm); textSize = 8f
                setTextColor(Color.parseColor("#BBBBBB")); gravity = Gravity.CENTER
            })
            cell.addView(TextView(this).apply {
                text = ScheduleData.formatTime(eh, em); textSize = 7f
                setTextColor(Color.parseColor("#D8D8D8")); gravity = Gravity.CENTER
            })
            lessonCol.addView(cell)
        }
        table.addView(lessonCol)

        dayCals.forEachIndexed { di, dayCal ->
            val (_, dy) = weekDates[di]
            val isToday = displayWeek == currentWeek && dayCal == currentDay
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(colW, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(dp(1), 0, dp(1), 0)
            }
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, headerH)
                setBackgroundColor(Color.WHITE)
            }
            header.addView(TextView(this).apply {
                text = "周${dayNames[di]}"; textSize = 10f; gravity = Gravity.CENTER
                setTextColor(if (isToday) Color.parseColor("#7890D8") else Color.parseColor("#AAAAAA"))
            })
            val frame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).also {
                    it.topMargin = dp(2); it.gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            if (isToday) {
                frame.addView(View(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(26), dp(26)).also {
                        it.gravity = Gravity.CENTER
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL; setColor(Color.parseColor("#7890D8"))
                    }
                })
            }
            frame.addView(TextView(this).apply {
                text = "$dy"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isToday) Color.WHITE else Color.parseColor("#333333"))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
            })
            header.addView(frame)
            col.addView(header)

            val activeCourses = ScheduleData.getCoursesForDay(displayWeek, dayCal)
            val allCoursesDay = ScheduleData.COURSES.filter { it.dayOfWeek == dayCal }
                .sortedBy { it.startLesson }
            val occupied = mutableSetOf<Int>()
            for (c in activeCourses) for (l in c.startLesson..c.endLesson) occupied.add(l)

            var lesson = 1
            while (lesson <= 12) {
                val active = activeCourses.firstOrNull { it.startLesson == lesson }
                if (active != null) {
                    col.addView(buildGridBlock(active, true, slotH))
                    lesson = active.endLesson + 1; continue
                }
                if (lesson in occupied) { lesson++; continue }
                val ghost = allCoursesDay
                    .filter { !it.isActiveInWeek(displayWeek) && it.startLesson == lesson }
                    .filter { (it.startLesson..it.endLesson).none { l -> l in occupied } }
                    .firstOrNull()
                if (ghost != null) {
                    col.addView(buildGridBlock(ghost, false, slotH))
                    lesson = ghost.endLesson + 1; continue
                }
                col.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, slotH)
                    setBackgroundColor(Color.parseColor("#F0F1F8"))
                })
                lesson++
            }
            table.addView(col)
        }
        return table
    }

    private fun buildGridBlock(course: Course, isActive: Boolean, slotH: Int): LinearLayout {
        val span   = course.endLesson - course.startLesson + 1
        val height = slotH * span - dp(1)
        val idx    = ScheduleData.COURSES.indexOf(course).coerceAtLeast(0)
        val scheme = colorSchemes[idx % colorSchemes.size]
        val bgAlpha = if (isActive) "88" else "22"
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), dp(4), dp(3), dp(3))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(scheme[1] + bgAlpha))
                cornerRadius = dp(6).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height).also { it.bottomMargin = dp(1) }
            if (!isActive) addView(TextView(this@MainActivity).apply {
                text = "非本周"; textSize = 7f; setTextColor(Color.parseColor("#CCCCCC"))
            })
            addView(TextView(this@MainActivity).apply {
                text = course.name
                textSize = if (span == 1) 8f else 9f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(if (isActive) scheme[3] else "#CCCCCC"))
                maxLines = if (span >= 3) 3 else 2
            })
            addView(TextView(this@MainActivity).apply {
                text = "@${course.location}"; textSize = 7f
                setTextColor(Color.parseColor(if (isActive) scheme[0] else "#DDDDDD"))
                setPadding(0, dp(1), 0, 0)
            })
        }
    }

    private fun formatHours(mins: Int): String {
        val m = maxOf(mins, 0); val h = m / 60; val min = m % 60
        return if (h > 0 && min > 0) "${h}.${min * 10 / 60}h"
        else if (h > 0) "${h}h" else "${min}m"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
