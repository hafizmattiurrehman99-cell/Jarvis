package com.example.jarvis

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class CommandHandler(private val context: Context, private val speakCallback: (String) -> Unit) {

    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, JarvisDeviceAdminReceiver::class.java)

    private val openWords = listOf(
        "khol do", "khol de", "khol", "kholo", "open kar", "open",
        "start kar", "start", "launch kar", "launch", "chalao", "chala do"
    )

    fun handleCommand(command: String) {
        when {
            command.contains("time") || command.contains("samay") || command.contains("waqt") -> {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                speak("Abhi time hai $time")
            }

            command.contains("date") || command.contains("tareekh") -> {
                val date = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
                speak("Aaj ki date hai $date")
            }

            command.contains("phone lock") || command.contains("screen lock") || command.contains("lock kar") -> {
                if (devicePolicyManager.isAdminActive(adminComponent)) {
                    devicePolicyManager.lockNow()
                    speak("Phone lock kar raha hoon")
                } else {
                    speak("Pehle mujhe lock karne ki permission dijiye")
                    requestDeviceAdmin()
                }
            }

            command.contains("whatsapp") && command.contains(" ko ") -> {
                sendWhatsappMessage(command)
            }

            command.contains("alarm") -> {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                speak("Alarm app khol raha hoon")
            }

            command.contains("camera") -> {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    speak("Camera khol raha hoon")
                }
            }

            command.contains("youtube") -> {
                val query = command.replace("youtube", "")
                    .replace(Regex("khol.*|kholo|open|search|karo|par"), "").trim()
                if (query.isEmpty()) {
                    if (!openAppByName("youtube")) speak("YouTube is phone par install nahi hai")
                } else {
                    val ytIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    try {
                        context.startActivity(ytIntent)
                        speak("YouTube par $query dhoondh raha hoon")
                    } catch (e: Exception) {
                        speak("YouTube nahi khul saka")
                    }
                }
            }

            command.contains("call") || command.contains("kaal") -> {
                val digits = command.filter { it.isDigit() }
                if (digits.length >= 6) {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$digits")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        context.startActivity(callIntent)
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
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra("query", query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                speak("Search kar raha hoon $query")
            }

            command.contains("background band") -> {
                // yeh JarvisListenerService khud handle karega
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

    private fun requestDeviceAdmin() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Jarvis ko phone lock karne ke liye yeh permission chahiye"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun sendWhatsappMessage(command: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            speak("Contacts padhne ki permission nahi mili")
            return
        }
        val parts = command.split(" ko ", limit = 2)
        if (parts.size < 2) {
            speak("Kisko message karna hai, naam samajh nahi aaya")
            return
        }
        val name = parts[0].trim()
        var message = parts[1]
            .replace("whatsapp par", "")
            .replace("whatsapp pe", "")
            .replace("whatsapp", "")
            .replace(Regex("\\bbolo\\b|\\bkaho\\b|\\blikho\\b|\\bmessage\\b|\\bkaro\\b|\\bbhejo\\b"), "")
            .trim()
        if (message.isEmpty()) message = "Hi"

        val number = getContactNumber(name)
        if (number == null) {
            speak("Mujhe \"$name\" naam ka contact nahi mila")
            return
        }
        val cleanNumber = number.replace(Regex("[^0-9+]"), "")
        val uri = Uri.parse("https://wa.me/$cleanNumber?text=" + Uri.encode(message))
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            speak("$name ke liye WhatsApp par message tayyar kar diya, ab Send dabaiye")
        } catch (e: Exception) {
            speak("WhatsApp nahi khul saka")
        }
    }

    private fun getContactNumber(name: String): String? {
        val cr = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        var number: String? = null
        val cursor = cr.query(uri, projection, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val displayName = it.getString(nameIndex) ?: ""
                if (displayName.lowercase(Locale.getDefault()).contains(name.lowercase(Locale.getDefault()))) {
                    number = it.getString(numberIndex)
                    return@use
                }
            }
        }
        return number
    }

    private fun openAppByName(spokenNameRaw: String): Boolean {
        val spokenName = spokenNameRaw.trim().lowercase(Locale.getDefault())
        if (spokenName.isEmpty()) return false
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val installedApps: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
        var bestMatch: ResolveInfo? = null
        for (app in installedApps) {
            val label = app.loadLabel(pm).toString().lowercase(Locale.getDefault())
            if (label == spokenName) { bestMatch = app; break }
        }
        if (bestMatch == null) {
            for (app in installedApps) {
                val label = app.loadLabel(pm).toString().lowercase(Locale.getDefault())
                if (label.contains(spokenName) || spokenName.contains(label)) { bestMatch = app; break }
            }
        }
        return if (bestMatch != null) {
            val packageName = bestMatch.activityInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                speak("${bestMatch.loadLabel(pm)} khol raha hoon")
                true
            } else false
        } else false
    }

    private fun speak(text: String) {
        speakCallback(text)
    }
}
