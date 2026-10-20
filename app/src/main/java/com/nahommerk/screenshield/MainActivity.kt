package com.nahommerk.screenshield

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView
    private lateinit var permText: TextView
    private val dynamicButtons = ArrayList<Pair<Button, () -> String>>()
    private val limits = intArrayOf(30, 45, 60, 90, 120, 180, 0) // 0 = unlimited
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        buildUi()
        maybeRequestNotifPermission()
    }
    
    override fun onResume() {
        super.onResume()
        refresh()
    }
    
    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(64, 96, 64, 64)
        }
        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        
        root.addView(TextView(this).apply {
            text = "\nScreenShield"
            textSize = 40f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        })
        
        root.addView(TextView(this).apply {
            text = "Screen time + brain-rot blocker"
            textSize = 18f
            setTextColor(Color.GRAY)
        })
        
        statusText = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 48, 0, 24)
        }
        root.addView(statusText)
        
        permText = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFFFFC107.toInt())
            setPadding(0, 0, 0, 32)
        }
        root.addView(permText)
        
        fun addButton(label: () -> String, onClick: () -> Unit) {
            val b = Button(this).apply { textSize = 20f }
            b.setOnClickListener { onClick(); refresh() }
            root.addView(b, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 24 })
            dynamicButtons.add(b to label)
        }
        
        addButton({ "Daily limit: ${limitLabel()}" }) { cycleLimit() }
        addButton({ "Time limit: ${onOff(prefs.timeLimitEnabled())}" }) {
            prefs.setTimeLimitEnabled(!prefs.timeLimitEnabled())
        }
        addButton({ "Brain-rot filter: ${onOff(prefs.filterEnabled())}" }) {
            prefs.setFilterEnabled(!prefs.filterEnabled())
        }
        addButton({ "Add keyword" }) { addKeyword() }
        addButton({ "Remove keyword" }) { removeKeyword() }
        addButton({ "Change PIN" }) { changePin() }
        addButton({ "Open accessibility settings" }) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        
        setContentView(scroll)
    }
    
    private fun refresh() {
        val watchedMin = prefs.watchedMs() / 60_000
        statusText.text = "Today: $watchedMin min watched \u2022 Limit: ${limitLabel()}"
        permText.text = buildString {
            append(if (UsageMonitorService.hasUsageAccess(this@MainActivity)) "\u2705" else "\u274C")
            append(" Usage access ")
            append(if (isFilterServiceEnabled()) "\u2705" else "\u274C")
            append(" Content filter service")
        }
        dynamicButtons.forEach { (b, label) -> b.text = label() }
        if (prefs.timeLimitEnabled()) UsageMonitorService.start(this)
        else UsageMonitorService.stop(this)
    }
    
    private fun limitLabel(): String {
        val m = prefs.limitMinutes()
        return if (m <= 0) "Unlimited" else "$m min"
    }
    
    private fun cycleLimit() {
        val idx = limits.indexOf(prefs.limitMinutes())
        val next = if (idx == -1) 0 else (idx + 1) % limits.size
        prefs.setLimitMinutes(limits[next])
    }
    
    private fun onOff(b: Boolean) = if (b) "ON" else "OFF"
    
    private fun addKeyword() {
        val input = EditText(this).apply { hint = "e.g. skibidi" }
        AlertDialog.Builder(this)
            .setTitle("Add keyword")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val k = input.text.toString().trim()
                if (k.isNotEmpty()) {
                    val set = prefs.keywords()
                    set.add(k)
                    prefs.setKeywords(set)
                    Toast.makeText(this, "Added \"$k\"", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun removeKeyword() {
        val list = prefs.keywords().sorted()
        if (list.isEmpty()) {
            Toast.makeText(this, "No keywords", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Tap a keyword to remove it")
            .setItems(list.toTypedArray()) { _, which ->
                val set = prefs.keywords()
                set.remove(list[which])
                prefs.setKeywords(set)
                Toast.makeText(this, "Removed \"${list[which]}\"", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun changePin() {
        val input = EditText(this).apply {
            hint = "4 digits"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("Change PIN")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val p = input.text.toString().trim()
                if (p.length == 4 && p.all { it.isDigit() }) {
                    prefs.setPin(p)
                    Toast.makeText(this, "PIN updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun isFilterServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName)
    }
    
    private fun maybeRequestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }
}
