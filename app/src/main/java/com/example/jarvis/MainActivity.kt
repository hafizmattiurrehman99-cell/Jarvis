package com.example.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView

    private val SPEECH_REQUEST_CODE = 100
    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val micButton: Button = findViewById(R.id.micButton)

        tts = TextToSpeech(this, this)

        requestNeededPermissions()

        micButton.setOnClickListener {
            startListening()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    private fun requestNeededPermissions() {
        val needed = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        ).filter {
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
            handleCommand(spokenText.lowercase(Locale.getDefault()))
        }
    }

    private val openWords = listOf("khol do", "khol de", "khol", "kholo", "open kar", "open", "start kar", "start", "launch kar", "launch", "chalao", "chala do")

    private fun handleCommand(command: String) {
        when {
            command.contains("time") || command.contains("samay") || command.contains("waqt") -> {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                speak("Abhi time hai $time")
            }

            command.contains("date") || command.contains("tareekh") -> {
                val date = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
                speak("Aaj ki date hai $date")
            }

            command.contains("alarm") -> {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                startActivity(intent)
                speak("Alarm app khol raha hoon")
            }

            command.contains("camera") -> {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    speak("Camera khol raha hoon")
                }
            }

            command.contains("youtube") -> {
                val query = command.replace("youtube", "")
                    .replace(Regex("khol.*|kholo|open|search|karo|par"), "").trim()
                if (query.isEmpty()) {
                    if (!openAppByName("youtube")) speak("YouTube is phone par install nahi hai")
                } else {
                    val ytIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)))
                    try {
                        startActivity(ytIntent)
                        speak("YouTube par $query dhoondh raha hoon")
                    } catch (e: Exception) {
                        speak("YouTube nahi khul saka")
                    }
                }
            }

            command.contains("call") || command.contains("kaal") -> {
                val digits = command.filter { it.isDigit() }
                if (digits.length >= 6) {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$digits"))
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        startActivity(callIntent)
                        speak("$digits par call kar raha hoon")
                    } else {
                        speak("Call ki permission nahi mili")
                    }
                } else {
                    speak("Number samajh nahi aaya, dobara boliye")
                }
            }

            command.contains("browser") || command.contains("search") -> {
                val query = command.replace("search", "").replace("browser", "").trim()
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra("query", query)
                startActivity(intent)
                speak("Search kar raha hoon $query")
            }

            command.contains("kaise ho") || command.contains("kya haal") || command.contains("how are you") -> {
                speak("Main bilkul theek hoon, aap bataiye main aapke liye kya kar sakta hoon")
            }

            command.contains("naam kya") || command.contains("tum kaun") || command.contains("aap kaun") -> {
                speak("Mera naam Jarvis hai, main aapka personal assistant hoon")
            }

            command.contains("shukriya") || command.contains("thank you") || command.contains("thanks") -> {
                speak("Koi baat nahi, hamesha khush rahiye")
            }

            command.contains("hello") || command.contains("hi") || command.contains("salam") -> {
                speak("Hello, main Jarvis hoon. Bataiye kya karna hai")
            }

            openWords.any { command.contains(it) } -> {
                var appName = command
                for (w in openWords) {
                    appName = appName.replace(w, "")
                }
                appName = appName.trim()
                if (appName.isEmpty()) {
                    speak("Kaunsi app kholni hai, naam boliye")
                } else if (!openAppByName(appName)) {
                    speak("Mujhe \"$appName\" naam ki app is phone par nahi mili")
                }
            }

            else -> {
                speak("Maaf kijiye, mujhe yeh command samajh nahi aayi")
            }
        }
    }

    private fun openAppByName(spokenNameRaw: String): Boolean {
        val spokenName = spokenNameRaw.trim().lowercase(Locale.getDefault())
        if (spokenName.isEmpty()) return false

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val installedApps: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)

        var bestMatch: ResolveInfo? = null

        for (app in installedApps) {
            val label = app.loadLabel(pm).toString().lowercase(Locale.getDefault())
            if (label == spokenName) {
                bestMatch = app
                break
            }
        }

        if (bestMatch == null) {
            for (app in installedApps) {
                val label = app.loadLabel(pm).toString().lowercase(Locale.getDefault())
                if (label.contains(spokenName) || spokenName.contains(label)) {
                    bestMatch = app
                    break
                }
            }
        }

        return if (bestMatch != null) {
            val packageName = bestMatch.activityInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
                speak("${bestMatch.loadLabel(pm)} khol raha hoon")
                true
            } else {
                false
            }
        } else {
            false
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
