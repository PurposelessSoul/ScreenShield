package com.nahommerk.screenshield

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BrainRotAccessibilityService : AccessibilityService() {
    private lateinit var prefs: Prefs
    private var lastBlockMs = 0L
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !this::prefs.isInitialized || !prefs.filterEnabled()) return
        // Cooldown so one bad video doesn't trigger a block loop.
        val now = System.currentTimeMillis()
        if (now - lastBlockMs < 15_000) return
        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collectText(root, texts, 0)
        if (texts.isEmpty()) return
        val match = KeywordFilter.match(texts.joinToString(" "), prefs.keywords().toList())
        if (match != null) {
            lastBlockMs = now
            performGlobalAction(GLOBAL_ACTION_HOME)
            BlockOverlayActivity.launchContent(this, match)
        }
    }
    
    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>, depth: Int) {
        if (depth > 25 || out.size > 400) return
        node.text?.let { out.add(it.toString()) }
        node.contentDescription?.let { out.add(it.toString()) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, out, depth + 1) }
        }
    }
    
    override fun onInterrupt() {}
}
