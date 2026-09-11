# Jarvis (Personal Use Android App)

Yeh ek basic Jarvis-style voice assistant hai jo sirf aapke apne Android device par
chalega. Play Store par publish karne ki zaroorat nahi.

## Kya-kya hai isme
- Mic button dabao -> bolo -> woh text mein badalta hai (Speech-to-Text)
- Command samajh kar kaam karta hai (time batana, date batana, alarm kholna,
  camera kholna, browser search karna)
- Jawab awaaz mein deta hai (Text-to-Speech)

## Isko chalane ke liye (step-by-step)

1. **Android Studio install karo** (agar pehle se nahi hai) - https://developer.android.com/studio
2. Android Studio khol kar **"Open"** karo aur is `JarvisApp` folder ko select karo.
3. Gradle sync hone do (thoda time lagega, internet chahiye pehli baar).
4. Apna phone USB se connect karo aur usme **Developer Options -> USB Debugging** on karo.
   (Settings > About Phone > 7 baar "Build Number" par tap karo, phir
   Settings > Developer Options me USB debugging on karo.)
5. Android Studio ke top par apna device select karo aur **Run (green play button)** dabao.
6. App seedha aapke phone par install ho jayegi.

## Agar USB cable nahi hai / direct APK chahiye
1. Android Studio me **Build > Build App Bundle(s) / APK(s) > Build APK(s)** dabao.
2. Ek `.apk` file banegi (`app/build/outputs/apk/debug/app-debug.apk`).
3. Yeh file apne phone me transfer karo (Google Drive, USB, ya email se).
4. Phone par us file par tap karo. Pehli baar phone puchega "Install from unknown
   sources" - use allow karo (Settings > Security me bhi manually on kar sakte ho).
5. Install hone ke baad app khol lo.

## Sirf phone se banana (bina laptop, bina terminal) — GitHub Actions ka tareeka

Is project me pehle se ek `.github/workflows/build.yml` file di gayi hai jo
GitHub par khud-ba-khud APK bana degi. Aapko sirf yeh karna hai:

1. **github.com** par jaake free account banao (agar nahi hai).
2. Naya repository banao — "New repository" > naam do `JarvisApp` > **Create**.
   (Public ya Private, dono chalega.)
3. Us repository ke andar **"Add file" > "Upload files"** par jao.
4. Is poore `JarvisApp` folder ke **andar ka saara content** (files aur folders,
   including `.github` folder, `app` folder, sab kuch) select karke upload kar do.
   (Phone ke file manager se "select all" karke upload kar sakte ho.)
5. Neeche **"Commit changes"** dabao.
6. Upar repository ke andar **"Actions"** tab par jao.
7. Ek build apne aap chalna shuru ho jayega (naam hoga "Build APK"). Usme click
   karke dekho — 3 se 8 minute lagte hain.
8. Jab build ke aage green tick ✅ aa jaye, usi build ke andar neeche
   **"Artifacts"** section me `jarvis-apk` naam ki file milegi — usko tap karke
   download kar lo (yeh ek zip hogi, jiske andar `app-debug.apk` hoga).
9. Us zip ko extract karo, `app-debug.apk` par tap karo, "Install unknown apps"
   allow karo, aur install kar lo.

Isme na terminal chalana hai, na command type karni hai — sirf files upload
karni hain aur GitHub khud APK bana kar de dega.

## Permissions
Pehli baar app kholne par yeh Microphone, Call, aur SMS ki permission maangega -
inhe "Allow" karna zaroori hai warna voice aur kuch actions kaam nahi karenge.

## Aage kya badhaya ja sakta hai
- Naye commands add karna: `MainActivity.kt` file me `handleCommand()` function
  ke andar naya `command.contains("...")` block add karo.
- AI se jodna: Chahe to Anthropic Claude API ko call karke zyada smart jawab
  bhi de sakte hain (internet permission already add hai).
- Background me hamesha chalna: Foreground Service ya WorkManager add karke
  isko continuously sunne wala assistant bhi bana sakte hain.
