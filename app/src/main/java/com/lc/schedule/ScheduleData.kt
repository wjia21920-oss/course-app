package com.lc.schedule

import java.util.Calendar

data class Course(
    val name: String,
    val startLesson: Int,
    val endLesson: Int,
    val dayOfWeek: Int,
    val startWeek: Int,
    val endWeek: Int,
    val location: String,
    val teacher: String
) {
    fun getStartTime(): Pair<Int, Int> = LESSON_TIMES[startLesson]!!.first
    fun getEndTime(): Pair<Int, Int> = LESSON_TIMES[endLesson]!!.second
    fun getStartMinutes(): Int { val (h, m) = getStartTime(); return h * 60 + m }
    fun getEndMinutes(): Int { val (h, m) = getEndTime(); return h * 60 + m }
    fun isActiveInWeek(week: Int) = week in startWeek..endWeek
}

val LESSON_TIMES = mapOf(
    1  to Pair(Pair(8,  0),  Pair(8,  40)),
    2  to Pair(Pair(8,  40), Pair(9,  20)),
    3  to Pair(Pair(9,  35), Pair(10, 15)),
    4  to Pair(Pair(10, 15), Pair(10, 55)),
    5  to Pair(Pair(11, 10), Pair(11, 50)),
    6  to Pair(Pair(11, 50), Pair(12, 30)),
    7  to Pair(Pair(14, 30), Pair(15, 10)),
    8  to Pair(Pair(15, 10), Pair(15, 50)),
    9  to Pair(Pair(16,  5), Pair(16, 45)),
    10 to Pair(Pair(16, 45), Pair(17, 25)),
    11 to Pair(Pair(19, 30), Pair(20, 10)),
    12 to Pair(Pair(20, 10), Pair(20, 50))
)

object ScheduleData {

    private val SEMESTER_START: Long by lazy {
        Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val COURSES = listOf(
        Course("微电影创作综合实训", 1, 4, Calendar.MONDAY,    17, 18, "H502",    "何嘉淇"),
        Course("新媒体运营推广",     3, 4, Calendar.MONDAY,     1, 16, "I-212",   "李玥"),
        Course("网络视频编辑",       5, 6, Calendar.MONDAY,     1, 16, "H404",    "高丽英"),
        Course("影视后期与节目制作", 9,10, Calendar.MONDAY,     1, 16, "H603",    "吴菁桐"),
        Course("影视后期与节目制作", 1, 2, Calendar.TUESDAY,    1, 16, "H404",    "吴菁桐"),
        Course("体育与健康三",       5, 6, Calendar.TUESDAY,    5, 17, "运动场57","谢迎晖"),
        Course("网络视频编辑",       7, 8, Calendar.TUESDAY,    1, 16, "H401",    "高丽英"),
        Course("微电影创作综合实训", 7,10, Calendar.TUESDAY,   17, 18, "H502",    "何嘉淇"),
        Course("网络视频编辑",       1, 2, Calendar.WEDNESDAY,  1, 16, "H501",   "高丽英"),
        Course("微电影创作综合实训", 1, 2, Calendar.WEDNESDAY, 17, 18, "H502",   "何嘉淇"),
        Course("毛概",               5, 6, Calendar.WEDNESDAY,  1, 14, "I-503",  "陈翠红"),
        Course("形势与政策",         5, 6, Calendar.WEDNESDAY, 15, 18, "I-503",  "朱文钧"),
        Course("新媒体运营推广",     3, 4, Calendar.THURSDAY,   1, 16, "I-212",  "李玥"),
        Course("走在前列的广东实践", 5, 6, Calendar.THURSDAY,   1,  8, "I-104",  "潘梅兰"),
        Course("大学生心理健康教育", 5, 6, Calendar.THURSDAY,  13, 16, "4-603",  "梁健欣"),
        Course("微电影创作综合实训", 5,10, Calendar.THURSDAY,  17, 18, "H502",   "何嘉淇"),
        Course("播音与主持",         9,10, Calendar.THURSDAY,   1, 16, "4-508",  "李玥"),
        Course("网络视频编辑",       1, 2, Calendar.FRIDAY,     1, 16, "H404",   "高丽英"),
        Course("微电影创作综合实训", 1, 4, Calendar.FRIDAY,    17, 18, "H502",   "何嘉淇"),
        Course("影视后期与节目制作", 5, 6, Calendar.FRIDAY,     1, 16, "H304",   "吴菁桐"),
        Course("新媒体运营推广",     9,10, Calendar.FRIDAY,     1, 16, "I-212",  "李玥")
    )

    fun getCurrentWeek(): Int {
        val now = System.currentTimeMillis()
        if (now < SEMESTER_START) return 0
        val diffDays = ((now - SEMESTER_START) / (1000L * 60 * 60 * 24)).toInt()
        return (diffDays / 7) + 1
    }

    fun getCoursesForDay(week: Int, dayOfWeek: Int): List<Course> {
        if (week <= 0 || week > 18) return emptyList()
        return COURSES.filter { it.dayOfWeek == dayOfWeek && it.isActiveInWeek(week) }
            .sortedBy { it.startLesson }
    }

    fun getTodayCourses(): List<Course> {
        val week = getCurrentWeek()
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return getCoursesForDay(week, day)
    }

    fun getCurrentCourse(): Course? {
        val mins = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        return getTodayCourses().firstOrNull { mins in it.getStartMinutes()..it.getEndMinutes() }
    }

    fun getNextCourse(): Course? {
        val mins = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        return getTodayCourses().firstOrNull { it.getStartMinutes() > mins }
    }

    fun formatTime(h: Int, m: Int) = "%02d:%02d".format(h, m)
}
