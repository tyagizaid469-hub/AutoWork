package com.geminiauto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GeminiAutoService : AccessibilityService() {

    companion object {
        private const val TAG = "GeminiAutoService"
        private var instance: GeminiAutoService? = null
        private var pendingAction: String? = null
        private var currentQuestion: String? = null

        fun execute(question: String) {
            currentQuestion = question
            pendingAction = "OPEN_GEMINI"
            instance?.performAction("OPEN_GEMINI")
        }

        fun isIdle(): Boolean = pendingAction == null
    }

    override fun onServiceConnected() {
        instance = this
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val action = pendingAction ?: return

        when (action) {
            "OPEN_GEMINI" -> {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    val packageName = event.packageName?.toString() ?: ""
                    if (packageName == Config.GEMINI_PACKAGE) {
                        pendingAction = "TYPE_QUESTION"
                        Log.d(TAG, "Gemini opened, now typing...")
                        performAction("TYPE_QUESTION")
                    }
                }
            }

            "TYPE_QUESTION" -> {
                val question = currentQuestion ?: return
                if (findAndTypeQuestion(question)) {
                    pendingAction = "SEND"
                    performAction("SEND")
                }
            }

            "SEND" -> {
                if (findAndClickSend()) {
                    Log.d(TAG, "Question sent successfully!")
                    pendingAction = null
                    currentQuestion = null
                    BackgroundService.onPromptDone()
                }
            }
        }
    }

    private fun findAndTypeQuestion(question: String): Boolean {
        rootInActiveWindow?.let { root ->
            val editTexts = mutableListOf<AccessibilityNodeInfo>()
            findEditTexts(root, editTexts)
            for (et in editTexts) {
                if (et.isVisibleToUser && et.isEnabled) {
                    val text = et.text?.toString() ?: ""
                    if (text.length < 100) {
                        et.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val args = android.os.Bundle().apply {
                                putString(
                                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                    question
                                )
                            }
                            et.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        }
                        Log.d(TAG, "Typed question: $question")
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun findAndClickSend(): Boolean {
        rootInActiveWindow?.let { root ->
            val clickable = mutableListOf<AccessibilityNodeInfo>()
            findClickableNodes(root, clickable)
            for (node in clickable) {
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                if (desc.contains("send") || text.contains("send") ||
                    desc.contains("submit") || text.contains("submit") ||
                    desc.contains("→") || text.contains("→")
                ) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(TAG, "Clicked send button")
                    return true
                }
            }
            for (node in clickable.reversed()) {
                if (node.className?.contains("ImageButton") == true ||
                    node.className?.contains("ImageView") == true
                ) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(TAG, "Clicked send (fallback)")
                    return true
                }
            }
        }
        return false
    }

    private fun findEditTexts(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.className?.contains("EditText") == true) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findEditTexts(it, result) }
        }
    }

    private fun findClickableNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findClickableNodes(it, result) }
        }
    }

    private fun performAction(action: String) {
        when (action) {
            "OPEN_GEMINI" -> {
                val intent = packageManager.getLaunchIntentForPackage(Config.GEMINI_PACKAGE)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    Log.d(TAG, "Opening Gemini app...")
                }
            }
            "TYPE_QUESTION" -> {
                rootInActiveWindow?.let {
                    findAndTypeQuestion(currentQuestion ?: "")
                }
            }
            "SEND" -> {
                findAndClickSend()
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
