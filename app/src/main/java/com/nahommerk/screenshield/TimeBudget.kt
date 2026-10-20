package com.nahommerk.screenshield

import java.util.Calendar

object TimeBudget {
    /** Local-calendar day key like "2026-10-19". */
    fun dayKey(nowMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
    /** limitMs <= 0 means unlimited and never blocks. */
    fun isExceeded(watchedMs: Long, limitMs: Long): Boolean =
        limitMs > 0 && watchedMs >= limitMs
}
