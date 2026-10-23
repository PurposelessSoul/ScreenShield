package com.nahommerk.screenshield

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences = 
        context.getSharedPreferences("screenshield", Context.MODE_PRIVATE)
    
    fun limitMinutes() = sp.getInt(K_LIMIT, 60)
    fun setLimitMinutes(v: Int) = sp.edit().putInt(K_LIMIT, v).apply()
    fun timeLimitEnabled() = sp.getBoolean(K_TIME_ON, true)
    fun setTimeLimitEnabled(v: Boolean) = sp.edit().putBoolean(K_TIME_ON, v).apply()
    fun filterEnabled() = sp.getBoolean(K_FILTER_ON, true)
    fun setFilterEnabled(v: Boolean) = sp.edit().putBoolean(K_FILTER_ON, v).apply()
    fun watchedMs() = sp.getLong(K_WATCHED, 0L)
    fun setWatchedMs(v: Long) = sp.edit().putLong(K_WATCHED, v).apply()
    fun lastDay(): String = sp.getString(K_DAY, "") ?: ""
    fun setLastDay(v: String) = sp.edit().putString(K_DAY, v).apply()
    fun graceUntil() = sp.getLong(K_GRACE, 0L)
    fun setGraceUntil(v: Long) = sp.edit().putLong(K_GRACE, v).apply()
    fun foregroundPackage(): String = sp.getString(K_FOREGROUND_PACKAGE, "") ?: ""
    fun foregroundPackageAt(): Long = sp.getLong(K_FOREGROUND_PACKAGE_AT, 0L)
    fun setForegroundPackage(packageName: String, at: Long) = sp.edit()
        .putString(K_FOREGROUND_PACKAGE, packageName)
        .putLong(K_FOREGROUND_PACKAGE_AT, at)
        .apply()
    fun pin(): String = sp.getString(K_PIN, "0000") ?: "0000"
    fun setPin(v: String) = sp.edit().putString(K_PIN, v).apply()
    fun keywords(): MutableSet<String> = 
        sp.getStringSet(K_KEYWORDS, null)?.toMutableSet() ?: 
        DEFAULT_KEYWORDS.toMutableSet()
    fun setKeywords(v: Set<String>) = sp.edit().putStringSet(K_KEYWORDS, v).apply()
    
    companion object {
        val DEFAULT_KEYWORDS = linkedSetOf(
            "skibidi", "toilet", "sigma", "rizz", "gyatt", "ohio", "fanum tax",
            "sussy", "among us", "brain rot", "grimace shake", "baby shark",
            "cocomelon", "goofy ahh"
        )
        private const val K_LIMIT = "limit_minutes"
        private const val K_TIME_ON = "time_limit_on"
        private const val K_FILTER_ON = "filter_on"
        private const val K_WATCHED = "watched_ms"
        private const val K_DAY = "last_day"
        private const val K_GRACE = "grace_until"
        private const val K_FOREGROUND_PACKAGE = "foreground_package"
        private const val K_FOREGROUND_PACKAGE_AT = "foreground_package_at"
        private const val K_PIN = "pin"
        private const val K_KEYWORDS = "keywords"
    }
}
