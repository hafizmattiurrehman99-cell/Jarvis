package com.example.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView
    private lateinit var commandHandler: CommandHandler
    private lateinit var backgroundButton: Button

    private val SPEECH_REQUEST_CODE = 100
    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val micButton: Button = findViewById(R.id.micButton)
        backgroundButton = findViewById(R.id.backgroundButton)

        tts = TextToSpeech(this, this)
        commandHandler = CommandHandler(this) { text -> speak(text) }

        requestNeededPermissions()
        updateBackgroundButtonLabel()

        micButton.setOnClickListener { startListening() }

        backgroundButton.setOnClickListener {
            if (JarvisListenerService.isRunning) {
                stopService(Intent(this, JarvisListenerService::class.java))
            } else {
                val intent = Intent(this, JarvisListenerService::class.java)
                ContextCompat.startForegroundService(this, intent)
            }
            backgroundButton.postDelayed({ updateBackgroundButtonLabel() }, 500)
        }
    }

    override fun onResume() {
        super.onResume()
        updateBackgroundButtonLabel()
    }

    private fun updateBackgroundButtonLabel() {
        backgroundButton.text = if (JarvisListenerService.isRunning)
            "Background: ON (band karne ke liye dabayein)"
        else
            "Background: OFF (shuru karne ke liye dabayein)"
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            tts.setPitch(0.75f)
            try {
                val maleVoice = tts.voices?.firstOrNull { voice ->
                    val name = voice.name.lowercase(Locale.getDefault())
                    name.contains("male") && !name.contains("female")
                }
                if (maleVoice != null) tts.voice = maleVoice
            } catch (e: Exception) {
            }
        }
    }

    private fun requestNeededPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bolo, main sun raha hoon...")
        }
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            speak("Speech recognition available nahi hai is device par")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            statusText.text = "Aapne kaha: $spokenText"
            commandHandler.handleCommand(spokenText.lowercase(Locale.getDefault()))
        }
    }

    private fun speak(text: String) {
        statusText.text = text
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
