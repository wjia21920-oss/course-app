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

    // p6风格配色：[顶部横条色, 背景色, 文字色]
    private val schemes = listOf(
        arrayOf("#A0C4E8", "#EAF3FC", "#3A6A90"),
        arrayOf("#F0A0B8", "#FCE8EF", "#A03858"),
        arrayOf("#90C8A0", "#E4F6EC", "#2A6840"),
        arrayOf("#E8C070", "#FAF0D0", "#806020"),
        arrayOf("#B8A0E0", "#EEE8FC", "#583888"),  // 换掉原来融合背景的紫色
        arrayOf("#70C0D8", "#E0F4FA", "#206880"),
        arrayOf("#E09890", "#FCE8E4", "#803830"),
        arrayOf("#90C890", "#E4F6E4", "#286828"),
        arrayOf("#E0B880", "#FAF0E0", "#805030"),
        arrayOf("#A8B8E8", "#EAEEfc", "#384888")
    )

    private var displayWeek = 0
    private var selectedDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    private var selectedDayOffset = 0
    private var selectedQueryWeek = 1

    private lateinit var weekLabel: TextView
    private lateinit var gridContainer: LinearLayout
    private lateinit var scheduleSection: LinearLayout
    private lateinit var dayContent: LinearLayout
    private lateinit var noteContent: LinearLayout
    private lateinit var noteInput: EditText
    private lateinit var mainTabViews: List<LinearLayout>
    private lateinit var subTabViews: List<TextView>
    private lateinit var dateSelectorRow: LinearLayout
    private lateinit var statsRow: LinearLayout

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
        selectedQueryWeek = displayWeek

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EEEEF6"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        }
        setContentView(root)

        // 不用ScrollView，直接LinearLayout，一屏看完不滚动
        val contentWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding(dp(12), dp(40), dp(12), 0)
        }

        // 顶部日期卡（紧凑版）
        contentWrap.addView(buildTopDateCard())

        // 日程+课表内容区
        scheduleSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scheduleSection.addView(buildSubTabBar())
        dateSelectorRow = buildDateSelectorRow()
        scheduleSection.addView(dateSelectorRow)

        dayContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = dp(6); it.bottomMargin = dp(4)
            }
        }
        noteContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))
            visibility = View.GONE
        }
        noteInput = EditText(this).apply {
            hint = "记点什么……"
            textSize = 13f
            setTextColor(Color.parseColor("#222222"))
            setHintTextColor(Color.parseColor("#BBBBBB"))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(14), dp(14), dp(14), dp(14))
            minLines = 6; gravity = Gravity.TOP
        }
        noteContent.addView(noteInput)

        scheduleSection.addView(dayContent)
        scheduleSection.addView(statsRow)
        scheduleSection.addView(noteContent)

        // 课表区
        gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        contentWrap.addView(scheduleSection)
        contentWrap.addView(gridContainer)

        root.addView(contentWrap)
        root.addView(buildBottomNav())

        updateMainTabStyles(0)
        updateSubTabStyles(0)
        refreshDayContent()
        buildGridPage()
    }

    // 顶部日期卡：紧凑，带阴影
    private fun buildTopDateCard(): LinearLayout {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val days = arrayOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val dayStr = days[cal.get(Calendar.DAY_OF_WEEK)]
        val week = ScheduleData.getCurrentWeek()
        val todayCount = if (week in 1..18)
            ScheduleData.getCoursesForDay(week, cal.get(Calendar.DAY_OF_WEEK)).size else 0
        val tomorrowCal = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowCount = if (week in 1..18)
            ScheduleData.getCoursesForDay(week, tomorrowCal.get(Calendar.DAY_OF_WEEK)).size else 0

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(18).toFloat()
            }
            elevation = dp(3).toFloat()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(8) }

            val left = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            left.addView(TextView(this@MainActivity).apply {
                text = "✦  今日日程"; textSize = 10f
                setTextColor(Color.parseColor("#AAAAAA"))
            })
            left.addView(TextView(this@MainActivity).apply {
                text = "${month}月${day}日"
                textSize = 24f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#111111")); setPadding(0, dp(2), 0, 0)
            })
            left.addView(TextView(this@MainActivity).apply {
                text = "$dayStr  ·  $todayCount 门课"
                textSize = 12f; setTextColor(Color.parseColor("#999999"))
                setPadding(0, dp(2), 0, 0)
            })
            addView(left)

            val rightCard = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#EEEEF8")); cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dp(60),
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            rightCard.addView(TextView(this@MainActivity).apply {
                text = "明日"; textSize = 10f
                setTextColor(Color.parseColor("#9090D8")); gravity = Gravity.CENTER
            })
            rightCard.addView(TextView(this@MainActivity).apply {
                text = "$tomorrowCount"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#9090D8")); gravity = Gravity.CENTER
            })
            rightCard.addView(TextView(this@MainActivity).apply {
                text = "门课"; textSize = 10f
                setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER
            })
            addView(rightCard)
        }
    }

    // 7天日期选择条：紧凑白卡
    private fun buildDateSelectorRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(14).toFloat()
            }
            elevation = dp(2).toFloat()
            setPadding(dp(6), dp(6), dp(6), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(6) }

            val days = arrayOf("", "日", "一", "二", "三", "四", "五", "六")
            for (offset in 0..6) {
                val c = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, offset) }
                val d = c.get(Calendar.DAY_OF_MONTH)
                val dow = c.get(Calendar.DAY_OF_WEEK)
                val isSelected = offset == selectedDayOffset

                val item = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(dp(1), dp(3), dp(1), dp(3))
                    setOnClickListener { selectDay(offset) }
                }
                item.addView(TextView(this@MainActivity).apply {
                    text = "周${days[dow]}"; textSize = 8f; gravity = Gravity.CENTER
                    setTextColor(if (isSelected) Color.parseColor("#9090D8")
                                 else Color.parseColor("#BBBBBB"))
                })
                val circle = FrameLayout(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).also {
                        it.topMargin = dp(2); it.gravity = Gravity.CENTER_HORIZONTAL
                    }
                }
                if (isSelected) {
                    circle.addView(View(this@MainActivity).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(26), dp(26)).also {
                            it.gravity = Gravity.CENTER
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#9090D8"))
                        }
                    })
                }
                circle.addView(TextView(this@MainActivity).apply {
                    text = "$d"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#333333"))
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT)
                })
                item.addView(circle)
                addView(item)
            }
        }
    }

    private fun selectDay(offset: Int) {
        selectedDayOffset = offset
        val c = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, offset) }
        selectedDayOfWeek = c.get(Calendar.DAY_OF_WEEK)
        val semStart = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 31, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        val diffDays = ((c.timeInMillis - semStart.timeInMillis) / 86400000L).toInt()
        selectedQueryWeek = if (diffDays >= 0) (diffDays / 7) + 1 else -1

        val parent = dateSelectorRow.parent as? LinearLayout ?: return
        val idx = parent.indexOfChild(dateSelectorRow)
        dateSelectorRow = buildDateSelectorRow()
        parent.removeViewAt(idx)
        parent.addView(dateSelectorRow, idx)
        refreshDayContent()
    }

    private fun getSelectedCal(): Calendar =
        Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, selectedDayOffset) }

    private fun refreshDayContent() {
        dayContent.removeAllViews()
        val courses = if (selectedQueryWeek in 1..18)
            ScheduleData.getCoursesForDay(selectedQueryWeek, selectedDayOfWeek)
        else emptyList()
        buildDayTimeline(courses)
        refreshStats(courses)
    }

    // 日程时间轴：紧凑版，p5渐变卡片
    private fun buildDayTimeline(courses: List<Course>) {
        val cal = Calendar.getInstance()
        val isToday = selectedDayOffset == 0
        val currentMins = if (isToday)
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE) else -1
        val days = arrayOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val dayStr = days[getSelectedCal().get(Calendar.DAY_OF_WEEK)]

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(16).toFloat()
            }
            elevation = dp(2).toFloat()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(6) }
        }

        // 标题行紧凑
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(8) }
        }
        titleRow.addView(TextView(this).apply {
            text = if (isToday) "今日日程" else "${dayStr}日程"
            textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#222222"))
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        card.addView(titleRow)

        if (courses.isEmpty()) {
            card.addView(TextView(this).apply {
                text = "这天没有课 ☀"
                textSize = 13f; setTextColor(Color.parseColor("#CCCCCC"))
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            dayContent.addView(card); return
        }

        courses.forEachIndexed { idx, course ->
            val (sh, sm) = course.getStartTime()
            val (eh, em) = course.getEndTime()
            val isCurrent = currentMins in course.getStartMinutes()..course.getEndMinutes()
            val isPast    = isToday && currentMins > course.getEndMinutes()
            val sc = schemes[idx % schemes.size]

            // 空闲
            if (idx > 0) {
                val gap = course.getStartMinutes() - courses[idx-1].getEndMinutes()
                if (gap > 0) {
                    val h = gap / 60; val m = gap % 60
                    card.addView(TextView(this).apply {
                        text = "空闲  ${if (h>0) "${h}h " else ""}${if (m>0) "${m}min" else ""}"
                        textSize = 10f; setTextColor(Color.parseColor("#CCCCCC"))
                        setPadding(dp(44), dp(2), 0, dp(2))
                    })
                }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(6) }
            }

            // 左侧时间+节次（紧凑）
            val leftCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(dp(44),
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            leftCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(sh, sm); textSize = 11f
                typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#222222"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            leftCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(eh, em); textSize = 9f; gravity = Gravity.END
                setTextColor(Color.parseColor("#CCCCCC")); setPadding(0, dp(1), 0, dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val lessonStr = "${course.startLesson}${if (course.endLesson > course.startLesson) "-${course.endLesson}" else ""}节"
            leftCol.addView(TextView(this).apply {
                text = lessonStr; textSize = 9f; gravity = Gravity.CENTER
                setPadding(dp(3), dp(2), dp(3), dp(2))
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#F0F0F0")
                             else Color.parseColor(sc[1]))
                    cornerRadius = dp(8).toFloat()
                }
                setTextColor(if (isPast) Color.parseColor("#BBBBBB") else Color.parseColor(sc[2]))
            })
            row.addView(leftCol)

            // 中间竖线+圆点（细）
            val lineCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(dp(16),
                    LinearLayout.LayoutParams.MATCH_PARENT)
                setPadding(0, dp(3), 0, 0)
            }
            lineCol.addView(View(this).apply {
                val sz = dp(8)
                layoutParams = LinearLayout.LayoutParams(sz, sz).also {
                    it.gravity = Gravity.CENTER_HORIZONTAL
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (isPast) Color.parseColor("#DDDDDD")
                             else Color.parseColor(sc[0]))
                }
            })
            val span = course.endLesson - course.startLesson + 1
            lineCol.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(2),
                    dp(44) + (span - 1) * dp(16)).also { it.gravity = Gravity.CENTER_HORIZONTAL }
                setBackgroundColor(if (isPast) Color.parseColor("#EEEEEE")
                                   else Color.parseColor(sc[0] + "55"))
            })
            row.addView(lineCol)

            // 右侧渐变课程卡（p5风格，紧凑）
            val courseCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = if (isPast) {
                    GradientDrawable().apply {
                        setColor(Color.parseColor("#F5F5F5")); cornerRadius = dp(12).toFloat()
                    }
                } else {
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(Color.parseColor(sc[1]),
                            Color.parseColor(sc[1] + "44").let {
                                // 右侧更淡
                                Color.argb(80,
                                    Color.red(Color.parseColor(sc[1])),
                                    Color.green(Color.parseColor(sc[1])),
                                    Color.blue(Color.parseColor(sc[1])))
                            })
                    ).apply { cornerRadius = dp(12).toFloat() }
                }
                elevation = if (isPast) 0f else dp(1).toFloat()
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = dp(6) }
                minimumHeight = dp(44) + (span - 1) * dp(16)
                setPadding(dp(10), dp(8), dp(10), dp(8))
            }
            // 顶部色条（p6风格）
            courseCard.addView(View(this).apply {
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#DDDDDD") else Color.parseColor(sc[0]))
                    cornerRadius = dp(2).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(3)).also {
                    it.bottomMargin = dp(6)
                }
            })
            if (isCurrent) {
                courseCard.addView(TextView(this).apply {
                    text = "▶ 进行中"; textSize = 8f
                    setTextColor(Color.parseColor(sc[2])); setPadding(0, 0, 0, dp(2))
                })
            }
            courseCard.addView(TextView(this).apply {
                text = course.name; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#BBBBBB") else Color.parseColor(sc[2]))
            })
            courseCard.addView(TextView(this).apply {
                text = "@${course.location}"; textSize = 10f
                setTextColor(if (isPast) Color.parseColor("#CCCCCC")
                             else Color.parseColor(sc[2] + "AA").let {
                                 Color.parseColor(sc[2])
                             })
                setPadding(0, dp(2), 0, 0)
                alpha = if (isPast) 1f else 0.7f
            })
            row.addView(courseCard)
            card.addView(row)
        }
        dayContent.addView(card)
    }

    private fun refreshStats(courses: List<Course>) {
        statsRow.removeAllViews()
        val totalMins = courses.sumOf { it.getEndMinutes() - it.getStartMinutes() }
        val freeMins  = if (courses.size > 1) maxOf(0,
            courses.last().getEndMinutes() - courses.first().getStartMinutes() - totalMins) else 0

        listOf(
            arrayOf("${courses.size}门", "今日门数", "#8888D0", "#EEEEF8"),
            arrayOf(formatHours(totalMins), "上课时长", "#60B080", "#E4F4EC"),
            arrayOf(formatHours(freeMins),  "空闲时间", "#E09040", "#FEF4E0")
        ).forEachIndexed { i, item ->
            val val1 = item[0]; val label = item[1]
            val textColor = item[2]; val bgColor = item[3]
            val statCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(bgColor)); cornerRadius = dp(14).toFloat()
                }
                elevation = dp(2).toFloat()
                setPadding(dp(6), dp(12), dp(6), dp(12))
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (i < 2) it.marginEnd = dp(6)
                }
            }
            statCard.addView(TextView(this).apply {
                text = val1; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(textColor)); gravity = Gravity.CENTER
            })
            statCard.addView(TextView(this).apply {
                text = label; textSize = 9f
                setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            })
            statsRow.addView(statCard)
        }
    }

    private fun buildSubTabBar(): LinearLayout {
        val bg = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E4E4EE")); cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(3), dp(3), dp(3), dp(3))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = dp(4); it.bottomMargin = dp(6)
            }
        }
        val names = listOf("日程", "备忘录")
        subTabViews = names.mapIndexed { i, name ->
            TextView(this).apply {
                text = name; textSize = 12f; gravity = Gravity.CENTER
                setPadding(0, dp(7), 0, dp(7))
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
        val items = listOf("日程", "课表")
        mainTabViews = items.mapIndexed { i, label ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(4), dp(6), dp(4), dp(4))
                setOnClickListener { switchMainTab(i) }
            }
            item.addView(TextView(this).apply {
                text = label; textSize = 13f; gravity = Gravity.CENTER; tag = "tab_$i"
            })
            item.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(4)).also {
                    it.topMargin = dp(3); it.gravity = Gravity.CENTER_HORIZONTAL
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT)
                }
                tag = "dot_$i"
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
            val active = i == selected
            item.findViewWithTag<TextView>("tab_$i")
                ?.setTextColor(Color.parseColor(if (active) "#333333" else "#BBBBBB"))
            item.findViewWithTag<TextView>("tab_$i")?.typeface =
                if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            (item.findViewWithTag<View>("dot_$i")?.background as? GradientDrawable)
                ?.setColor(if (active) Color.parseColor("#9090D8") else Color.TRANSPARENT)
        }
    }

    private fun switchSubTab(index: Int) {
        updateSubTabStyles(index)
        dayContent.visibility  = if (index == 0) View.VISIBLE else View.GONE
        statsRow.visibility    = if (index == 0) View.VISIBLE else View.GONE
        noteContent.visibility = if (index == 1) View.VISIBLE else View.GONE
    }

    private fun updateSubTabStyles(selected: Int) {
        subTabViews.forEachIndexed { i, tv ->
            if (i == selected) {
                tv.setTextColor(Color.parseColor("#222222")); tv.typeface = Typeface.DEFAULT_BOLD
                tv.background = GradientDrawable().apply {
                    setColor(Color.WHITE); cornerRadius = dp(10).toFloat()
                }
            } else {
                tv.setTextColor(Color.parseColor("#AAAAAA")); tv.typeface = Typeface.DEFAULT
                tv.background = null
            }
        }
    }

    private fun buildGridPage() {
        gridContainer.removeAllViews()

        // p6风格：周次栏纯文字，无胶囊背景
        val weekBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        weekLabel = TextView(this).apply {
            text = "第${displayWeek}周"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#222222")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        weekBar.addView(TextView(this).apply {
            text = "＜"; textSize = 16f; setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(dp(4), dp(2), dp(10), dp(2))
            setOnClickListener { if (displayWeek > 1) { displayWeek--; refreshGrid() } }
        })
        weekBar.addView(weekLabel)
        weekBar.addView(TextView(this).apply {
            text = "＞"; textSize = 16f; setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(dp(10), dp(2), dp(4), dp(2))
            setOnClickListener { if (displayWeek < 18) { displayWeek++; refreshGrid() } }
        })
        weekBar.addView(TextView(this).apply {
            text = "本周"; textSize = 10f; setTextColor(Color.parseColor("#9090D8"))
            setPadding(dp(8), dp(2), 0, dp(2))
            setOnClickListener {
                displayWeek = ScheduleData.getCurrentWeek().coerceAtLeast(1); refreshGrid()
            }
        })
        gridContainer.addView(weekBar)

        // 细分割线
        gridContainer.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#EEEEEE"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        })

        val tableScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        tableScroll.addView(buildWeekGrid())
        gridContainer.addView(tableScroll)
    }

    private fun refreshGrid() {
        weekLabel.text = "第${displayWeek}周"
        if (gridContainer.childCount > 2) gridContainer.removeViewAt(2)
        val tableScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        tableScroll.addView(buildWeekGrid())
        gridContainer.addView(tableScroll)
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
        val slotH = dp(50); val headerH = dp(40); val leftW = dp(34)

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.parseColor("#EEEEF6"))
        }

        // 左侧节次列：白底，节次数字+时间
        val lessonCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(leftW, LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.WHITE)
        }
        lessonCol.addView(TextView(this).apply {
            text = "${weekDates[0].first}月"; textSize = 8f
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
                text = "$i"; textSize = 10f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#888888")); gravity = Gravity.CENTER
            })
            cell.addView(TextView(this).apply {
                text = ScheduleData.formatTime(sh, sm); textSize = 7f
                setTextColor(Color.parseColor("#BBBBBB")); gravity = Gravity.CENTER
            })
            lessonCol.addView(cell)
        }
        table.addView(lessonCol)

        val dayWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        dayCals.forEachIndexed { di, dayCal ->
            val (_, dy) = weekDates[di]
            val isToday = displayWeek == currentWeek && dayCal == currentDay
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(1), 0, dp(1), 0)
            }

            // 表头：p6风格，无背景，周几+日期
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, headerH)
                setBackgroundColor(Color.WHITE)
            }
            header.addView(TextView(this).apply {
                text = "周${dayNames[di]}"; textSize = 9f; gravity = Gravity.CENTER
                setTextColor(if (isToday) Color.parseColor("#9090D8")
                             else Color.parseColor("#AAAAAA"))
            })
            val frame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).also {
                    it.topMargin = dp(1); it.gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            if (isToday) {
                frame.addView(View(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(22), dp(22)).also {
                        it.gravity = Gravity.CENTER
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#9090D8"))
                    }
                })
            }
            frame.addView(TextView(this).apply {
                text = "$dy"; textSize = 11f; typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isToday) Color.WHITE else Color.parseColor("#333333"))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
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
                    setBackgroundColor(Color.parseColor("#EEEEF6"))
                })
                lesson++
            }
            dayWrap.addView(col)
        }
        table.addView(dayWrap)
        return table
    }

    // p6风格课程块：顶部彩色横条+浅背景+深色字
    private fun buildGridBlock(course: Course, isActive: Boolean, slotH: Int): LinearLayout {
        val span   = course.endLesson - course.startLesson + 1
        val height = slotH * span - dp(1)
        val idx    = ScheduleData.COURSES.indexOf(course).coerceAtLeast(0)
        val sc     = schemes[idx % schemes.size]
        val bgColor = if (isActive) Color.parseColor(sc[1])
                      else Color.argb(60,
                          Color.red(Color.parseColor(sc[1])),
                          Color.green(Color.parseColor(sc[1])),
                          Color.blue(Color.parseColor(sc[1])))

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(bgColor); cornerRadius = dp(6).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height).also { it.bottomMargin = dp(1) }
            setPadding(dp(3), 0, dp(3), dp(3))

            // 顶部彩色横条（p6风格）
            addView(View(this@MainActivity).apply {
                background = GradientDrawable().apply {
                    setColor(if (isActive) Color.parseColor(sc[0])
                             else Color.parseColor("#CCCCCC"))
                    val r = dp(6).toFloat()
                    // 只上圆角
                    setCornerRadii(floatArrayOf(r,r,r,r,0f,0f,0f,0f))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(3))
            })

            val textWrap = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(2), dp(3), dp(2), 0)
            }
            if (!isActive) textWrap.addView(TextView(this@MainActivity).apply {
                text = "非本周"; textSize = 6f; setTextColor(Color.parseColor("#BBBBBB"))
            })
            textWrap.addView(TextView(this@MainActivity).apply {
                text = course.name
                textSize = if (span == 1) 8f else 9f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isActive) Color.parseColor(sc[2]) else Color.parseColor("#CCCCCC"))
                maxLines = if (span >= 3) 3 else 2
            })
            textWrap.addView(TextView(this@MainActivity).apply {
                text = "@${course.location}"; textSize = 7f
                setTextColor(if (isActive) Color.parseColor(sc[2])
                             else Color.parseColor("#DDDDDD"))
                alpha = if (isActive) 0.7f else 1f
                setPadding(0, dp(1), 0, 0)
            })
            addView(textWrap)
        }
    }

    private fun formatHours(mins: Int): String {
        val m = maxOf(mins, 0); val h = m / 60; val min = m % 60
        return if (h > 0 && min > 0) "${h}.${min * 10 / 60}h"
        else if (h > 0) "${h}h" else "${min}m"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
