package com.geminiauto

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Config {
    const val GEMINI_PACKAGE = "com.google.android.apps.gemini"
    private const val PREFS_NAME = "gemini_auto"
    private const val KEY_PROMPTS = "prompts"

    fun savePrompts(context: Context, prompts: List<Prompt>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (p in prompts) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("text", p.text)
            obj.put("interval", p.interval)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PROMPTS, arr.toString()).apply()
    }

    fun loadPrompts(context: Context): MutableList<Prompt> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROMPTS, "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<Prompt>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Prompt(
                id = obj.optLong("id", System.currentTimeMillis()),
                text = obj.optString("text", ""),
                interval = obj.optInt("interval", 30)
            ))
        }
        return list
    }

    fun addPrompt(context: Context, p: Prompt) {
        val list = loadPrompts(context)
        list.add(p)
        savePrompts(context, list)
    }

    fun removePrompt(context: Context, id: Long) {
        val list = loadPrompts(context)
        list.removeAll { it.id == id }
        savePrompts(context, list)
    }
}
