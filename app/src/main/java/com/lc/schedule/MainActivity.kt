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

    private val courseColorMap = mapOf(
        "微电影创作综合实训" to "#F7C5C5",
        "新媒体运营推广"     to "#C5D8F7",
        "网络视频编辑"       to "#D8C5F7",
        "影视后期与节目制作" to "#C5F7D8",
        "体育与健康三"       to "#F7ECC5",
        "毛概"               to "#F7C5D8",
        "形势与政策"         to "#F7D8C5",
        "走在前列的广东实践" to "#C5F7F0",
        "大学生心理健康教育" to "#E8C5F7",
        "播音与主持"         to "#F7C5EC"
    )

    private fun courseColor(name: String): String =
        courseColorMap.entries.firstOrNull { name.contains(it.key) || it.key.contains(name) }?.value
            ?: "#E8E8F0"

    private var displayWeek = 0
    private lateinit var weekLabel: TextView
    private lateinit var gridContainer: LinearLayout
    private lateinit var todayContent: LinearLayout
    private lateinit var noteInput: EditText
    private lateinit var tabViews: List<TextView>
    private lateinit var currentBar: LinearLayout
    private lateinit var contentPages: List<View>

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
            setBackgroundColor(Color.parseColor("#F5F5F7"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        }
        setContentView(root)

        root.addView(buildTopBar())

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val contentWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        todayContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }
        gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val noteWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
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

        contentPages = listOf(todayContent, gridContainer, noteWrap)
        contentWrap.addView(todayContent)
        contentWrap.addView(gridContainer)
        contentWrap.addView(noteWrap)
        scroll.addView(contentWrap)
        root.addView(scroll)

        currentBar = buildCurrentBar()
        root.addView(currentBar)

        root.addView(buildBottomNav())

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
        val week = ScheduleData.getCurrentWeek()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(48), dp(20), dp(16))
            addView(TextView(this@MainActivity).apply {
                text = "Hi，${month}月${day}日 $todayStr"
                textSize = 24f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#111111"))
            })
            addView(TextView(this@MainActivity).apply {
                text = when {
                    week <= 0  -> "还没开学"
                    week > 18  -> "学期结束"
                    else       -> "第${week}周 · 今日${ScheduleData.getTodayCourses().size}门课"
                }
                textSize = 12f; setTextColor(Color.parseColor("#999999"))
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun buildBottomNav(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(20))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            elevation = dp(12).toFloat()
        }
        val icons  = listOf("📅", "📋", "📝")
        val labels = listOf("日程", "课表", "备忘录")
        tabViews = labels.mapIndexed { i, name ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, dp(4), 0, dp(4))
                setOnClickListener { switchTab(i) }
            }
            col.addView(TextView(this).apply {
                text = icons[i]; textSize = 20f; gravity = Gravity.CENTER
            })
            val label = TextView(this).apply {
                text = name; textSize = 10f; gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            }
            col.addView(label)
            nav.addView(col)
            label
        }
        updateTabStyles(0)
        return nav
    }

    private fun buildCurrentBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(10), dp(20), dp(10))
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun updateCurrentBar() {
        currentBar.removeAllViews()
        val current = ScheduleData.getCurrentCourse()
        val next    = ScheduleData.getNextCourse()
        val course  = current ?: next ?: return
        val color   = courseColor(course.name)
        currentBar.background = GradientDrawable().apply {
            setColor(Color.parseColor(color + "66"))
        }
        currentBar.addView(View(this).apply {
            val size = dp(8)
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginEnd = dp(10); it.gravity = Gravity.CENTER_VERTICAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (current != null) Color.parseColor("#4CAF89")
                else Color.parseColor("#AAAAAA"))
            }
        })
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(this).apply {
            text = course.name; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111111"))
        })
        col.addView(TextView(this).apply {
            val (sh, sm) = course.getStartTime(); val (eh, em) = course.getEndTime()
            text = "${course.location}  ${ScheduleData.formatTime(sh,sm)}–${ScheduleData.formatTime(eh,em)}"
            textSize = 11f; setTextColor(Color.parseColor("#777777"))
        })
        currentBar.addView(col)
        currentBar.addView(TextView(this).apply {
            text = if (current != null) "进行中" else "即将开始"
            textSize = 12f
            setTextColor(if (current != null) Color.parseColor("#4CAF89")
            else Color.parseColor("#AAAAAA"))
        })
    }

    private fun switchTab(index: Int) {
        updateTabStyles(index)
        contentPages.forEachIndexed { i, v ->
            v.visibility = if (i == index) View.VISIBLE else View.GONE
        }
    }

    private fun updateTabStyles(selected: Int) {
        tabViews.forEachIndexed { i, tv ->
            tv.setTextColor(if (i == selected) Color.parseColor("#5B6FD8")
            else Color.parseColor("#AAAAAA"))
            tv.typeface = if (i == selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun buildTodayPage() {
        todayContent.removeAllViews()
        val courses = ScheduleData.getTodayCourses()
        val nowMins = Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

        val tomorrowCourses = run {
            val week = ScheduleData.getCurrentWeek()
            val cal  = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, 1)
            ScheduleData.getCoursesForDay(week, cal.get(Calendar.DAY_OF_WEEK))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(16) }
        }
        headerRow.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@MainActivity).apply {
                text = "✦  今日日程"; textSize = 12f; setTextColor(Color.parseColor("#8B8FB8"))
            })
            addView(TextView(this@MainActivity).apply {
                text = "${courses.size} 门课"; textSize = 13f
                setTextColor(Color.parseColor("#888888")); setPadding(0, dp(4), 0, 0)
            })
        })
        headerRow.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(20), dp(10), dp(20), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EEF0FA")); cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.marginStart = dp(12) }
            addView(TextView(this@MainActivity).apply {
                text = "明日"; textSize = 10f; setTextColor(Color.parseColor("#8B8FB8"))
                gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = "${tomorrowCourses.size}"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#5B6FD8")); gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = "门课"; textSize = 10f; setTextColor(Color.parseColor("#8B8FB8"))
                gravity = Gravity.CENTER
            })
        })
        todayContent.addView(headerRow)

        if (courses.isEmpty()) {
            todayContent.addView(TextView(this).apply {
                text = "今天没有课 ☕️"; textSize = 16f
                setTextColor(Color.parseColor("#BBBBBB")); gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            return
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(20).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(16) }
        }
        card.addView(TextView(this).apply {
            text = "今日日程"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111111"))
        })
        card.addView(TextView(this).apply {
            text = "按上课时间顺序展示今天的课程安排"
            textSize = 11f; setTextColor(Color.parseColor("#AAAACC"))
            setPadding(0, dp(4), 0, dp(16))
        })

        courses.forEachIndexed { idx, course ->
            val (sh, sm) = course.getStartTime()
            val (eh, em) = course.getEndTime()
            val startMins = sh * 60 + sm; val endMins = eh * 60 + em
            val isCurrent = nowMins in startMins..endMins
            val isPast    = nowMins > endMins
            val color     = courseColor(course.name)

            if (idx > 0) {
                val gap = startMins - courses[idx-1].getEndMinutes()
                if (gap > 0) {
                    val h = gap/60; val m = gap%60
                    card.addView(TextView(this).apply {
                        text = "  空闲 ${if(h>0)"${h}h " else ""}${if(m>0)"${m}min" else ""}"
                        textSize = 10f; setTextColor(Color.parseColor("#CCCCCC"))
                        setPadding(dp(80), dp(4), 0, dp(4))
                    })
                }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(10) }
            }
            val timeCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.END
                layoutParams = LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, dp(4), dp(10), 0)
            }
            timeCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(sh, sm); textSize = 13f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#222222"))
                gravity = Gravity.END
            })
            timeCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(eh, em); textSize = 10f
                setTextColor(Color.parseColor("#BBBBBB"))
                gravity = Gravity.END; setPadding(0, dp(2), 0, dp(6))
            })
            timeCol.addView(TextView(this).apply {
                text = "第${course.startLesson}-${course.endLesson}节"
                textSize = 9f; gravity = Gravity.CENTER
                setPadding(dp(6), dp(2), dp(6), dp(2))
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#555555"))
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#F0F0F4")
                    else Color.parseColor(color + "66"))
                    cornerRadius = dp(8).toFloat()
                }
            })

            row.addView(timeCol)
            row.addView(View(this).apply {
                val sz = if (isCurrent) dp(10) else dp(8)
                layoutParams = LinearLayout.LayoutParams(sz, sz).also {
                    it.topMargin = dp(5); it.marginEnd = dp(8)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (isPast) Color.parseColor("#DDDDDD")
                    else Color.parseColor(color))
                }
            })

            val courseCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(14), dp(14), dp(14))
                background = GradientDrawable().apply {
                    if (!isPast) {
                        colors = intArrayOf(Color.parseColor(color), Color.parseColor(color + "88"))
                        gradientType = GradientDrawable.LINEAR_GRADIENT
                        orientation = GradientDrawable.Orientation.LEFT_RIGHT
                    } else {
                        setColor(Color.parseColor("#F5F5F5"))
                    }
                    cornerRadius = dp(14).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            courseCard.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(3),
                    LinearLayout.LayoutParams.MATCH_PARENT).also { it.marginEnd = dp(10) }
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#DDDDDD") else Color.WHITE)
                    cornerRadius = dp(2).toFloat()
                }
            })
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(this).apply {
                text = course.name; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#AAAAAA") else Color.parseColor("#333333"))
            })
            textCol.addView(TextView(this).apply {
                text = "${ScheduleData.formatTime(sh,sm)} – ${ScheduleData.formatTime(eh,em)}"
                textSize = 11f
                setTextColor(if (isPast) Color.parseColor("#BBBBBB") else Color.parseColor("#555555"))
                setPadding(0, dp(3), 0, 0)
            })
            textCol.addView(TextView(this).apply {
                text = "${course.location}  ${course.teacher}"; textSize = 11f
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#666666"))
                setPadding(0, dp(2), 0, 0)
            })
            if (isCurrent) {
                textCol.addView(TextView(this).apply {
                    text = "▶ 进行中"; textSize = 10f
                    setTextColor(Color.parseColor("#5B6FD8")); setPadding(0, dp(4), 0, 0)
                })
            }
            courseCard.addView(textCol)
            row.addView(courseCard)
            card.addView(row)
        }
        todayContent.addView(card)

        val totalMins = courses.sumOf { it.getEndMinutes() - it.getStartMinutes() }
        val freeMins  = maxOf((courses.last().getEndMinutes() - courses.first().getStartMinutes()) - totalMins, 0)
        val statsRow  = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        listOf(
            Triple("${courses.size}", "今日门数", "#5B6FD8" to "#EEF0FA"),
            Triple(formatHours(totalMins), "上课时长", "#4CAF89" to "#EAF5F0"),
            Triple(formatHours(freeMins), "空闲时间", "#D4A853" to "#FBF4E8")
        ).forEachIndexed { i, (v, label, colors) ->
            statsRow.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(12), dp(14), dp(12), dp(14))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(colors.second)); cornerRadius = dp(16).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (i < 2) it.marginEnd = dp(10)
                }
                addView(TextView(this@MainActivity).apply {
                    text = v; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor(colors.first)); gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = label; textSize = 10f; setTextColor(Color.parseColor("#AAAACC"))
                    gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0)
                })
            })
        }
        todayContent.addView(statsRow)
    }

    private fun buildGridPage() {
        gridContainer.removeAllViews()
        val weekBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        weekBar.addView(TextView(this).apply {
            text = "＜"; textSize = 18f; setTextColor(Color.parseColor("#999999"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { if (displayWeek > 1) { displayWeek--; refreshGrid() } }
        })
        weekLabel = TextView(this).apply {
            text = "第${displayWeek}周"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#222222")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        weekBar.addView(weekLabel)
        weekBar.addView(TextView(this).apply {
            text = "＞"; textSize = 18f; setTextColor(Color.parseColor("#999999"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { if (displayWeek < 18) { displayWeek++; refreshGrid() } }
        })
        weekBar.addView(TextView(this).apply {
            text = "本周"; textSize = 11f; setTextColor(Color.parseColor("#5B6FD8"))
            setPadding(dp(12), dp(4), dp(4), dp(4))
            setOnClickListener {
                displayWeek = ScheduleData.getCurrentWeek().coerceAtLeast(1); refreshGrid()
            }
        })
        gridContainer.addView(weekBar)
        gridContainer.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        })
        gridContainer.addView(buildWeekGrid())
    }

    private fun refreshGrid() {
        weekLabel.text = "第${displayWeek}周"
        val old = gridContainer.getChildAt(2)
        if (old != null) gridContainer.removeView(old)
        gridContainer.addView(buildWeekGrid())
    }

    private fun buildWeekGrid(): LinearLayout {
        val dayNames = listOf("一", "二", "三", "四", "五")
        val dayCals  = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY)
        val curWeek  = ScheduleData.getCurrentWeek()
        val curDay   = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val cellH    = dp(68)

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#FAFAFA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // 节次列
        val lessonCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(32), LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.WHITE)
        }
        lessonCol.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(44))
        })
        for (i in 1..10) {
            val (sh, sm) = LESSON_TIMES[i]!!.first
            lessonCol.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(dp(32), cellH)
                setPadding(0, dp(6), 0, 0)
                addView(TextView(this@MainActivity).apply {
                    text = "$i"; textSize = 11f
                    setTextColor(Color.parseColor("#999999")); gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = "%02d:%02d".format(sh, sm); textSize = 8f
                    setTextColor(Color.parseColor("#CCCCCC")); gravity = Gravity.CENTER
                })
            })
        }
        table.addView(lessonCol)

        dayCals.forEachIndexed { di, dayCal ->
            val isToday = displayWeek == curWeek && dayCal == curDay
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(2), 0, dp(2), 0)
            }
            col.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
                if (isToday) {
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#5B6FD8")); cornerRadius = dp(10).toFloat()
                    }
                }
                addView(TextView(this@MainActivity).apply {
                    text = dayNames[di]; textSize = 13f
                    typeface = if (isToday) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextColor(if (isToday) Color.WHITE else Color.parseColor("#666666"))
                    gravity = Gravity.CENTER
                })
            })

            val courses = ScheduleData.getCoursesForDay(displayWeek, dayCal)
            var lesson = 1
            while (lesson <= 10) {
                val course = courses.firstOrNull { it.startLesson == lesson }
                if (course != null) {
                    val span = course.endLesson - course.startLesson + 1
                    val color = courseColor(course.name)
                    val (sh, sm) = course.getStartTime()
                    val (eh, em) = course.getEndTime()
                    col.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(4), dp(5), dp(4), dp(4))
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor(color)); cornerRadius = dp(10).toFloat()
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, cellH * span - dp(2)).also {
                            it.bottomMargin = dp(2)
                        }
                        addView(TextView(this@MainActivity).apply {
                            text = course.name; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
                            setTextColor(Color.parseColor("#333333")); maxLines = 3
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "${ScheduleData.formatTime(sh,sm)}–${ScheduleData.formatTime(eh,em)}"
                            textSize = 7f; setTextColor(Color.parseColor("#555555"))
                            setPadding(0, dp(2), 0, 0)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = course.location; textSize = 7f
                            setTextColor(Color.parseColor("#666666"))
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = course.teacher; textSize = 7f
                            setTextColor(Color.parseColor("#777777"))
                        })
                    })
                    lesson = course.endLesson + 1
                } else {
                    col.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, cellH - dp(2)).also {
                            it.bottomMargin = dp(2)
                        }
                        setBackgroundColor(Color.parseColor("#F8F8F8"))
                    })
                    lesson++
                }
            }
            table.addView(col)
        }
        return table
    }

    private fun formatHours(mins: Int): String {
        val m = maxOf(mins, 0); val h = m/60; val min = m%60
        return if (h>0 && min>0) "${h}.${min*10/60}h"
        else if (h>0) "${h}h" else "${min}m"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
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

    // 每门课专属颜色
    private val courseColorMap = mapOf(
        "微电影创作综合实训" to "#F7C5C5",
        "新媒体运营推广"     to "#C5D8F7",
        "网络视频编辑"       to "#D8C5F7",
        "影视后期与节目制作" to "#C5F7D8",
        "体育与健康三"       to "#F7ECC5",
        "毛概"               to "#F7C5D8",
        "形势与政策"         to "#F7D8C5",
        "走在前列的广东实践" to "#C5F7F0",
        "大学生心理健康教育" to "#E8C5F7",
        "播音与主持"         to "#F7C5EC"
    )

    private fun courseColor(name: String): String =
        courseColorMap.entries.firstOrNull { name.contains(it.key) || it.key.contains(name) }?.value
            ?: courseColorMap[name] ?: "#E8E8F0"

    private var displayWeek = 0
    private lateinit var weekLabel: TextView
    private lateinit var gridContainer: LinearLayout
    private lateinit var todayContent: LinearLayout
    private lateinit var noteInput: EditText
    private lateinit var tabViews: List<TextView>
    private lateinit var currentBar: LinearLayout
    private lateinit var contentPages: List<View>

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
            setBackgroundColor(Color.parseColor("#F5F5F7"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        }
        setContentView(root)

        // 顶部标题
        root.addView(buildTopBar())

        // 内容区
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val contentWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        todayContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }
        gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val noteWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
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

        contentPages = listOf(todayContent, gridContainer, noteWrap)
        contentWrap.addView(todayContent)
        contentWrap.addView(gridContainer)
        contentWrap.addView(noteWrap)
        scroll.addView(contentWrap)
        root.addView(scroll)

        // 当前课程条
        currentBar = buildCurrentBar()
        root.addView(currentBar)

        // 底部tab
        root.addView(buildBottomNav())

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
        val week = ScheduleData.getCurrentWeek()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(48), dp(20), dp(16))
            addView(TextView(this@MainActivity).apply {
                text = "Hi，${month}月${day}日 $todayStr"
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#111111"))
            })
            addView(TextView(this@MainActivity).apply {
                text = when {
                    week <= 0  -> "还没开学"
                    week > 18  -> "学期结束"
                    else       -> "第${week}周 · 今日${ScheduleData.getTodayCourses().size}门课"
                }
                textSize = 12f
                setTextColor(Color.parseColor("#999999"))
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun buildBottomNav(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(20))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            elevation = dp(12).toFloat()
        }
        val icons = listOf("📅", "📋", "📝")
        val labels = listOf("日程", "课表", "备忘录")
        tabViews = labels.mapIndexed { i, name ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, dp(4), 0, dp(4))
                setOnClickListener { switchTab(i) }
            }
            col.addView(TextView(this).apply {
                text = icons[i]; textSize = 20f; gravity = Gravity.CENTER
            })
            val label = TextView(this).apply {
                text = name; textSize = 10f; gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            }
            col.addView(label)
            nav.addView(col)
            label
        }
        updateTabStyles(0)
        return nav
    }

    private fun buildCurrentBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(10), dp(20), dp(10))
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun updateCurrentBar() {
        currentBar.removeAllViews()
        val current = ScheduleData.getCurrentCourse()
        val next    = ScheduleData.getNextCourse()
        val course  = current ?: next ?: return

        val color = courseColor(course.name)
        currentBar.background = GradientDrawable().apply {
            setColor(Color.parseColor(color + "66"))
        }
        currentBar.addView(View(this).apply {
            val size = dp(8)
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginEnd = dp(10); it.gravity = Gravity.CENTER_VERTICAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (current != null) Color.parseColor("#4CAF89")
                else Color.parseColor("#AAAAAA"))
            }
        })
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(this).apply {
            text = course.name; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111111"))
        })
        col.addView(TextView(this).apply {
            val (sh, sm) = course.getStartTime(); val (eh, em) = course.getEndTime()
            text = "${course.location}  ${ScheduleData.formatTime(sh,sm)}–${ScheduleData.formatTime(eh,em)}"
            textSize = 11f; setTextColor(Color.parseColor("#777777"))
        })
        currentBar.addView(col)
        currentBar.addView(TextView(this).apply {
            text = if (current != null) "进行中" else "即将开始"
            textSize = 12f
            setTextColor(if (current != null) Color.parseColor("#4CAF89")
            else Color.parseColor("#AAAAAA"))
        })
    }

    private fun switchTab(index: Int) {
        updateTabStyles(index)
        contentPages.forEachIndexed { i, v -> v.visibility = if (i == index) View.VISIBLE else View.GONE }
    }

    private fun updateTabStyles(selected: Int) {
        tabViews.forEachIndexed { i, tv ->
            tv.setTextColor(if (i == selected) Color.parseColor("#5B6FD8")
            else Color.parseColor("#AAAAAA"))
            tv.typeface = if (i == selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    // ==================== 日程页 ====================
    private fun buildTodayPage() {
        todayContent.removeAllViews()
        val courses = ScheduleData.getTodayCourses()
        val nowMins = Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

        // 明日预告小卡
        val tomorrowCourses = run {
            val week = ScheduleData.getCurrentWeek()
            val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, 1)
            ScheduleData.getCoursesForDay(week, cal.get(Calendar.DAY_OF_WEEK))
        }
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(16) }
        }
        headerRow.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@MainActivity).apply {
                text = "✦  今日日程"; textSize = 12f; setTextColor(Color.parseColor("#8B8FB8"))
            })
            addView(TextView(this@MainActivity).apply {
                text = "${courses.size} 门课"; textSize = 13f; setTextColor(Color.parseColor("#888888"))
                setPadding(0, dp(4), 0, 0)
            })
        })
        // 明日小卡
        headerRow.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(10), dp(20), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EEF0FA")); cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.marginStart = dp(12)
            }
            addView(TextView(this@MainActivity).apply {
                text = "明日"; textSize = 10f; setTextColor(Color.parseColor("#8B8FB8"))
                gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = "${tomorrowCourses.size}"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#5B6FD8")); gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = "门课"; textSize = 10f; setTextColor(Color.parseColor("#8B8FB8"))
                gravity = Gravity.CENTER
            })
        })
        todayContent.addView(headerRow)

        if (courses.isEmpty()) {
            todayContent.addView(TextView(this).apply {
                text = "今天没有课 ☕️"
                textSize = 16f; setTextColor(Color.parseColor("#BBBBBB"))
                gravity = Gravity.CENTER; setPadding(0, dp(40), 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            return
        }

        // 时间轴卡片
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(20).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(16) }
        }
        card.addView(TextView(this).apply {
            text = "今日日程"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111111"))
        })
        card.addView(TextView(this).apply {
            text = "按上课时间顺序展示今天的课程安排"
            textSize = 11f; setTextColor(Color.parseColor("#AAAACC"))
            setPadding(0, dp(4), 0, dp(16))
        })

        courses.forEachIndexed { idx, course ->
            val (sh, sm) = course.getStartTime()
            val (eh, em) = course.getEndTime()
            val startMins = sh * 60 + sm; val endMins = eh * 60 + em
            val isCurrent = nowMins in startMins..endMins
            val isPast    = nowMins > endMins
            val color     = courseColor(course.name)

            // 空闲
            if (idx > 0) {
                val gap = startMins - courses[idx-1].getEndMinutes()
                if (gap > 0) {
                    val h = gap/60; val m = gap%60
                    card.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(80), dp(4), 0, dp(4))
                        addView(TextView(this@MainActivity).apply {
                            text = "空闲 ${if(h>0)"${h}h " else ""}${if(m>0)"${m}min" else ""}"
                            textSize = 10f; setTextColor(Color.parseColor("#CCCCCC"))
                        })
                    })
                }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(10) }
            }

            // 左侧时间列
            val timeCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.END
                layoutParams = LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, dp(4), dp(10), 0)
            }
            timeCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(sh, sm)
                textSize = 13f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#222222"))
                gravity = Gravity.END
            })
            timeCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(eh, em)
                textSize = 10f; setTextColor(Color.parseColor("#BBBBBB"))
                gravity = Gravity.END; setPadding(0, dp(2), 0, dp(6))
            })
            // 节次标签
            timeCol.addView(TextView(this).apply {
                text = "第${course.startLesson}-${course.endLesson}节"
                textSize = 9f; gravity = Gravity.CENTER
                setPadding(dp(6), dp(2), dp(6), dp(2))
                setTextColor(if (isPast) Color.parseColor("#CCCCCC")
                else Color.parseColor(color.replace("#", "#")))
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#F0F0F4")
                    else Color.parseColor(color + "66"))
                    cornerRadius = dp(8).toFloat()
                }
            })

            // 圆点
            row.addView(timeCol)
            row.addView(View(this).apply {
                val sz = if (isCurrent) dp(10) else dp(8)
                layoutParams = LinearLayout.LayoutParams(sz, sz).also {
                    it.topMargin = dp(5); it.marginEnd = dp(8)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (isPast) Color.parseColor("#DDDDDD")
                    else Color.parseColor(color))
                }
            })

            // 课程卡片 - 带渐变
            val courseCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(14), dp(14), dp(14))
                background = GradientDrawable().apply {
                    if (!isPast) {
                        colors = intArrayOf(
                            Color.parseColor(color),
                            Color.parseColor(color + "88")
                        )
                        gradientType = GradientDrawable.LINEAR_GRADIENT
                        orientation = GradientDrawable.Orientation.LEFT_RIGHT
                    } else {
                        setColor(Color.parseColor("#F5F5F5"))
                    }
                    cornerRadius = dp(14).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            // 左侧色条
            courseCard.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(3), LinearLayout.LayoutParams.MATCH_PARENT).also {
                    it.marginEnd = dp(10)
                }
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#DDDDDD")
                    else Color.WHITE)
                    cornerRadius = dp(2).toFloat()
                }
            })
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(this).apply {
                text = course.name; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#AAAAAA") else Color.parseColor("#333333"))
            })
            textCol.addView(TextView(this).apply {
                text = "${ScheduleData.formatTime(sh,sm)} – ${ScheduleData.formatTime(eh,em)}"
                textSize = 11f
                setTextColor(if (isPast) Color.parseColor("#BBBBBB") else Color.parseColor("#555555"))
                setPadding(0, dp(3), 0, 0)
            })
            textCol.addView(TextView(this).apply {
                text = "${course.location}  ${course.teacher}"
                textSize = 11f
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#666666"))
                setPadding(0, dp(2), 0, 0)
            })
            if (isCurrent) {
                textCol.addView(TextView(this).apply {
                    text = "▶ 进行中"; textSize = 10f
                    setTextColor(Color.parseColor("#5B6FD8")); setPadding(0, dp(4), 0, 0)
                })
            }
            courseCard.addView(textCol)
            row.addView(courseCard)
            card.addView(row)
        }
        todayContent.addView(card)

        // 统计卡片
        val totalMins = courses.sumOf { it.getEndMinutes() - it.getStartMinutes() }
        val freeMins  = maxOf((courses.last().getEndMinutes() - courses.first().getStartMinutes()) - totalMins, 0)
        val statsRow  = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        listOf(
            Triple("${courses.size}", "今日门数", "#5B6FD8" to "#EEF0FA"),
            Triple(formatHours(totalMins), "上课时长", "#4CAF89" to "#EAF5F0"),
            Triple(formatHours(freeMins), "空闲时间", "#D4A853" to "#FBF4E8")
        ).forEachIndexed { i, (v, label, colors) ->
            statsRow.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(12), dp(14), dp(12), dp(14))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(colors.second)); cornerRadius = dp(16).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (i < 2) it.marginEnd = dp(10)
                }
                addView(TextView(this@MainActivity).apply {
                    text = v; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor(colors.first)); gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = label; textSize = 10f
                    setTextColor(Color.parseColor("#AAAACC"))
                    gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0)
                })
            })
        }
        todayContent.addView(statsRow)
    }

    // ==================== 课表页 ====================
    private fun buildGridPage() {
        gridContainer.removeAllViews()
        val weekBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        weekBar.addView(TextView(this).apply {
            text = "＜"; textSize = 18f; setTextColor(Color.parseColor("#999999"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { if (displayWeek > 1) { displayWeek--; refreshGrid() } }
        })
        weekLabel = TextView(this).apply {
            text = "第${displayWeek}周"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#222222")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        weekBar.addView(weekLabel)
        weekBar.addView(TextView(this).apply {
            text = "＞"; textSize = 18f; setTextColor(Color.parseColor("#999999"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { if (displayWeek < 18) { displayWeek++; refreshGrid() } }
        })
        weekBar.addView(TextView(this).apply {
            text = "本周"; textSize = 11f; setTextColor(Color.parseColor("#5B6FD8"))
            setPadding(dp(12), dp(4), dp(4), dp(4))
            setOnClickListener {
                displayWeek = ScheduleData.getCurrentWeek().coerceAtLeast(1); refreshGrid()
            }
        })
        gridContainer.addView(weekBar)
        gridContainer.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        })
        val hs = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        hs.addView(buildWeekGrid())
        gridContainer.addView(hs)
    }

    private fun refreshGrid() {
        weekLabel.text = "第${displayWeek}周"
        val hs = gridContainer.getChildAt(2) as? HorizontalScrollView ?: return
        hs.removeAllViews(); hs.addView(buildWeekGrid())
    }

    private fun buildWeekGrid(): LinearLayout {
        val dayNames  = listOf("一", "二", "三", "四", "五")
        val dayCals   = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY)
        val curWeek   = ScheduleData.getCurrentWeek()
        val curDay    = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val cellH     = dp(68) // 每节高度

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#FAFAFA"))
        }

        // 节次列
        val lessonCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(32), LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.WHITE)
        }
        lessonCol.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(44))
        })
        for (i in 1..10) {
            val (sh, sm) = LESSON_TIMES[i]!!.first
            lessonCol.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(dp(32), cellH)
                setPadding(0, dp(6), 0, 0)
                addView(TextView(this@MainActivity).apply {
                    text = "$i"; textSize = 11f
                    setTextColor(Color.parseColor("#999999")); gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = "%02d:%02d".format(sh, sm); textSize = 8f
                    setTextColor(Color.parseColor("#CCCCCC")); gravity = Gravity.CENTER
                })
            })
        }
        table.addView(lessonCol)

        // 每天
        dayCals.forEachIndexed { di, dayCal ->
            val isToday = displayWeek == curWeek && dayCal == curDay
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(dp(76), LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(dp(2), 0, dp(2), 0)
            }
            // 表头
            col.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
                if (isToday) {
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#5B6FD8"))
                        cornerRadius = dp(10).toFloat()
                    }
                }
                addView(TextView(this@MainActivity).apply {
                    text = dayNames[di]; textSize = 13f
                    typeface = if (isToday) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextColor(if (isToday) Color.WHITE else Color.parseColor("#666666"))
                    gravity = Gravity.CENTER
                })
            })

            val courses = ScheduleData.getCoursesForDay(displayWeek, dayCal)
            var lesson = 1
            while (lesson <= 10) {
                val course = courses.firstOrNull { it.startLesson == lesson }
                if (course != null) {
                    val span    = course.endLesson - course.startLesson + 1
                    val color   = courseColor(course.name)
                    val (sh, sm) = course.getStartTime()
                    val (eh, em) = course.getEndTime()
                    col.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(5), dp(6), dp(5), dp(4))
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor(color)); cornerRadius = dp(10).toFloat()
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, cellH * span - dp(2)).also {
                            it.bottomMargin = dp(2)
                        }
                        addView(TextView(this@MainActivity).apply {
                            text = course.name; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
                            setTextColor(Color.parseColor("#333333")); maxLines = 3
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "${ScheduleData.formatTime(sh,sm)}\n–${ScheduleData.formatTime(eh,em)}"
                            textSize = 8f; setTextColor(Color.parseColor("#555555"))
                            setPadding(0, dp(3), 0, 0)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = course.teacher; textSize = 8f
                            setTextColor(Color.parseColor("#777777"))
                            setPadding(0, dp(2), 0, 0)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = course.location; textSize = 8f
                            setTextColor(Color.parseColor("#888888"))
                        })
                    })
                    lesson = course.endLesson + 1
                } else {
                    col.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, cellH - dp(2)).also {
                            it.bottomMargin = dp(2)
                        }
                        setBackgroundColor(Color.parseColor("#F8F8F8"))
                    })
                    lesson++
                }
            }
            table.addView(col)
        }
        return table
    }

    private fun formatHours(mins: Int): String {
        val m = maxOf(mins, 0); val h = m/60; val min = m%60
        return if (h>0 && min>0) "${h}.${min*10/60}h"
        else if (h>0) "${h}h" else "${min}m"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
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
                week <= 0  -> "还没开学"
                week > 18  -> "学期结束"
                else       -> "第${week}周 · 今日${ScheduleData.getTodayCourses().size}门课"
            }
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            setPadding(0, dp(4), 0, 0)
        })
        bar.addView(left)
        return bar
    }

    private fun buildCurrentBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(12), dp(20), dp(24))
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            elevation = dp(8).toFloat()
        }
        return bar
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
        val wrap = todayContent.parent as LinearLayout
        todayContent.visibility  = if (index == 0) View.VISIBLE else View.GONE
        gridContainer.visibility = if (index == 1) View.VISIBLE else View.GONE
        wrap.getChildAt(2).visibility = if (index == 2) View.VISIBLE else View.GONE
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
        val freeMins = maxOf((courses.last().getEndMinutes() - courses.first().getStartMinutes()) - totalMins, 0)

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = dp(20) }
        }
        listOf(
            Triple("${courses.size}", "门课", "#5B6FD8"),
            Triple(formatHours(totalMins), "上课", "#4CAF89"),
            Triple(formatHours(freeMins), "空闲", "#D4A853")
        ).forEachIndexed { i, (v, label, color) ->
            statsRow.addView(LinearLayout(this).apply {
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
                addView(TextView(this@MainActivity).apply {
                    text = v; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor(color)); gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = label; textSize = 10f
                    setTextColor(Color.parseColor("#BBBBBB"))
                    gravity = Gravity.CENTER; setPadding(0, dp(2), 0, 0)
                })
            })
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
                text = ScheduleData.formatTime(sh, sm)
                textSize = 12f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPast) Color.parseColor("#CCCCCC") else Color.parseColor("#333333"))
                gravity = Gravity.END
            })
            timeCol.addView(TextView(this).apply {
                text = ScheduleData.formatTime(eh, em)
                textSize = 10f
                setTextColor(Color.parseColor("#CCCCCC"))
                gravity = Gravity.END; setPadding(0, dp(2), 0, 0)
            })

            row.addView(timeCol)
            row.addView(View(this).apply {
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
            })

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    setColor(if (isPast) Color.parseColor("#F5F5F5") else Color.WHITE)
                    cornerRadius = dp(12).toFloat()
                    if (!isPast) setStroke(dp(1), Color.parseColor(courseColors[idx % courseColors.size]))
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
            row.addView(card)
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
        weekBar.addView(TextView(this).apply {
            text = "＜"; textSize = 16f; setTextColor(Color.parseColor("#999999"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { if (displayWeek > 1) { displayWeek--; refreshGrid() } }
        })
        weekLabel = TextView(this).apply {
            text = "第${displayWeek}周"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#222222")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        weekBar.addView(weekLabel)
        weekBar.addView(TextView(this).apply {
            text = "＞"; textSize = 16f; setTextColor(Color.parseColor("#999999"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { if (displayWeek < 18) { displayWeek++; refreshGrid() } }
        })
        weekBar.addView(TextView(this).apply {
            text = "本周"; textSize = 11f; setTextColor(Color.parseColor("#5B6FD8"))
            setPadding(dp(10), dp(4), dp(4), dp(4))
            setOnClickListener { displayWeek = ScheduleData.getCurrentWeek().coerceAtLeast(1); refreshGrid() }
        })
        gridContainer.addView(weekBar)
        gridContainer.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        })
        val tableScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
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

    private fun buildWeekGrid(): LinearLayout {
        val dayNames = listOf("一", "二", "三", "四", "五")
        val dayCals  = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY)
        val currentWeek = ScheduleData.getCurrentWeek()
        val currentDay  = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val lessonCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(36), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        lessonCol.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(36), dp(40)) })
        for (i in 1..10) {
            lessonCol.addView(TextView(this).apply {
                text = "$i"; textSize = 10f; setTextColor(Color.parseColor("#CCCCCC"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(52))
            })
        }
        table.addView(lessonCol)

        dayCals.forEachIndexed { di, dayCal ->
            val isToday = displayWeek == currentWeek && dayCal == currentDay
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(dp(2), 0, dp(2), 0)
            }
            col.addView(TextView(this).apply {
                text = dayNames[di]; textSize = 12f; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
                setTextColor(if (isToday) Color.parseColor("#5B6FD8") else Color.parseColor("#999999"))
                typeface = if (isToday) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            })

            val courses = ScheduleData.getCoursesForDay(displayWeek, dayCal)
            var lesson = 1
            while (lesson <= 10) {
                val course = courses.firstOrNull { it.startLesson == lesson }
                if (course != null) {
                    val span = course.endLesson - course.startLesson + 1
                    val colorHex = courseColors[courses.indexOf(course) % courseColors.size]
                    col.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(4), dp(4), dp(4), dp(4))
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor(colorHex + "99"))
                            cornerRadius = dp(8).toFloat()
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(52) * span).also {
                            it.bottomMargin = dp(1)
                        }
                        addView(TextView(this@MainActivity).apply {
                            text = course.name; textSize = 9f
                            setTextColor(Color.parseColor("#333333"))
                            typeface = Typeface.DEFAULT_BOLD; maxLines = 3
                        })
                        addView(TextView(this@MainActivity).apply {
                            val (sh, sm) = course.getStartTime()
                            text = "${ScheduleData.formatTime(sh,sm)}\n${course.location}"
                            textSize = 8f; setTextColor(Color.parseColor("#666666"))
                            setPadding(0, dp(2), 0, 0)
                        })
                    })
                    lesson = course.endLesson + 1
                } else {
                    col.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).also { it.bottomMargin = dp(1) }
                        setBackgroundColor(Color.parseColor("#F8F8F8"))
                    })
                    lesson++
                }
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
