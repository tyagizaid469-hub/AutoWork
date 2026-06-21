package com.geminiauto

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var etQuestion: EditText
    private lateinit var etInterval: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView
    private lateinit var rvPrompts: RecyclerView
    private lateinit var adapter: PromptAdapter
    private val prompts = mutableListOf<Prompt>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etQuestion = findViewById(R.id.etQuestion)
        etInterval = findViewById(R.id.etInterval)
        btnAdd = findViewById(R.id.btnAdd)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        tvStatus = findViewById(R.id.tvStatus)
        rvPrompts = findViewById(R.id.rvPrompts)

        checkPermissions()
        loadPrompts()
        setupRecyclerView()

        btnAdd.setOnClickListener {
            val text = etQuestion.text.toString().trim()
            if (text.isBlank()) {
                Toast.makeText(this, "Question empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val interval = etInterval.text.toString().toIntOrNull() ?: 30
            val p = Prompt(text = text, interval = interval)
            Config.addPrompt(this, p)
            prompts.add(p)
            adapter.update(prompts)
            etQuestion.text.clear()
            etInterval.text.clear()
            Toast.makeText(this, "Prompt added!", Toast.LENGTH_SHORT).show()
        }

        btnStart.setOnClickListener {
            if (prompts.isEmpty()) {
                Toast.makeText(this, "Add at least 1 prompt", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isAccessibilityEnabled()) {
                showAccessibilityDialog()
                return@setOnClickListener
            }
            startAutomation()
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, BackgroundService::class.java))
            updateStatus("Stopped")
        }
    }

    private fun setupRecyclerView() {
        adapter = PromptAdapter(prompts) { p ->
            Config.removePrompt(this, p.id)
            prompts.removeAll { it.id == p.id }
            adapter.update(prompts)
        }
        rvPrompts.layoutManager = LinearLayoutManager(this)
        rvPrompts.adapter = adapter
    }

    private fun loadPrompts() {
        prompts.clear()
        prompts.addAll(Config.loadPrompts(this))
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = android.net.Uri.parse("package:$packageName")
            })
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains("$packageName/$packageName.GeminiAutoService") == true
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Accessibility Service Required")
            .setMessage("Go to Settings > Accessibility > Gemini Auto and enable it.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startAutomation() {
        if (prompts.isEmpty()) return
        Config.savePrompts(this, prompts)
        val intent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateStatus("Running... (${prompts.size} prompts)")
    }

    private fun updateStatus(text: String) {
        tvStatus.text = "Status: $text"
    }
}
