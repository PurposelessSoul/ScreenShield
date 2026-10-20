package com.nahommerk.screenshield

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class BlockOverlayActivity : Activity() {
    private lateinit var prefs: Prefs
    private val entered = StringBuilder()
    private lateinit var pinDisplay: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val reason = intent.getStringExtra(EXTRA_REASON) ?: REASON_TIME
        val keyword = intent.getStringExtra(EXTRA_KEYWORD)
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(48, 48, 48, 48)
        }
        
        root.addView(TextView(this).apply {
            text = if (reason == REASON_TIME) "\uD83D\uDD0D" else "\uD83D\uDEAB"
            textSize = 72f
            gravity = Gravity.CENTER
        })
        
        root.addView(TextView(this).apply {
            text = if (reason == REASON_TIME) "Screen time is over for today!"
                else "Blocked! This looks like brain rot."
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        })
        
        if (keyword != null) {
            root.addView(TextView(this).apply {
                text = "Blocked word: \"$keyword\""
                textSize = 20f
                setTextColor(0xFFFF6E6E.toInt())
                gravity = Gravity.CENTER
            })
        }
        
        root.addView(TextView(this).apply {
            text = "Parent? Enter PIN to unlock (+15 min)"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 16)
        })
        
        pinDisplay = TextView(this).apply {
            text = "_ _ _ _"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        root.addView(pinDisplay)
        
        // Remote-friendly numeric keypad (1-9, backspace, 0, OK)
        val grid = GridLayout(this).apply { columnCount = 3 }
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "\u232B", "0", "OK")
        for (k in keys) {
            val b = Button(this).apply {
                text = k
                textSize = 22f
                setOnClickListener { onKey(k) }
            }
            grid.addView(b, GridLayout.LayoutParams().apply {
                width = 200
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            })
        }
        root.addView(grid, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        
        setContentView(root)
        
        // Content blocks auto-dismiss after 30 s; time blocks need a PIN.
        if (reason == REASON_CONTENT) {
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 30_000)
        }
    }
    
    private fun onKey(k: String) {
        when {
            k == "OK" -> { checkPin(); return }
            k == "\u232B" && entered.isNotEmpty() -> entered.deleteCharAt(entered.length - 1)
            k != "\u232B" && entered.length < 4 -> entered.append(k)
        }
        updatePinDisplay()
    }
    
    private fun updatePinDisplay() {
        pinDisplay.text = (0 until 4).joinToString(" ") {
            if (it < entered.length) "\u2022" else "_"
        }
    }
    
    private fun checkPin() {
        if (entered.toString() == prefs.pin()) {
            if ((intent.getStringExtra(EXTRA_REASON) ?: REASON_TIME) == REASON_TIME) {
                prefs.setGraceUntil(System.currentTimeMillis() + 15 * 60_000L)
            }
            finish()
        } else {
            entered.clear()
            updatePinDisplay()
            Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        const val EXTRA_REASON = "reason"
        const val EXTRA_KEYWORD = "keyword"
        const val REASON_TIME = "time"
        const val REASON_CONTENT = "content"
        
        fun launchTimeUp(context: Context) = launch(context, REASON_TIME, null)
        fun launchContent(context: Context, keyword: String) = launch(context, REASON_CONTENT, keyword)
        
        private fun launch(context: Context, reason: String, keyword: String?) {
            context.startActivity(
                Intent(context, BlockOverlayActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_REASON, reason)
                    .putExtra(EXTRA_KEYWORD, keyword)
            )
        }
    }
}
