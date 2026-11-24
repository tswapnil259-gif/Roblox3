# Roblox AI Studio Assistant (V5.0.0)

Native Android app (Kotlin + Jetpack Compose + Material 3 + Room + Android Keystore) that helps you design Roblox games with AI.

**Important:** This app does **not** run Roblox Studio on Android. It generates Luau scripts, project organization, error fixes, and 3D concept prompts that you export and use inside Roblox Studio on Windows/macOS.

## Features

- Multi-provider AI: OpenRouter (free models), OpenAI, Anthropic Claude, DeepSeek
- Secure on-device API key storage (Android Keystore AES-256)
- Optional Node.js backend relay
- Script Generator (Script / LocalScript / ModuleScript)
- Game Builder presets (NPC, Quest, Inventory, Powers, Shop, XP, DataStore)
- Luau code editor with syntax highlighting
- Error Fixer (paste Studio output → corrected script)
- 3D Model concept + AI generator prompts + import guide
- Project manager with Room persistence

## Build APK with GitHub Actions (recommended)

1. Create a new GitHub repository.
2. Upload **all** files from this folder (keep the folder structure).
3. Push to `main`.
4. Open the **Actions** tab → workflow **Build Roblox AI Studio Assistant APK (V5)** runs automatically.
5. Download the artifact `RobloxAIStudioAssistant-v5.0.0-debug` → extract `app-debug.apk`.

The workflow uses:
- JDK 17
- Gradle 8.9 (compatible with AGP 8.7.3)
- Android SDK Platform 35
- Auto-generates `gradlew` if the wrapper jar is missing

## Local build (Android Studio or CLI)

```bash
# Generate wrapper once (if needed)
gradle wrapper --gradle-version 8.9

./gradlew testDebugUnitTest assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Optional Backend Relay

```bash
cd backend
npm install
OPENAI_API_KEY=... ANTHROPIC_API_KEY=... DEEPSEEK_API_KEY=... OPENROUTER_API_KEY=... \
RELAY_AUTH_TOKEN=optional-secret npm start
```

Then enable **Backend Relay Mode** in the app Settings and enter the public HTTPS URL.

## Tech stack

- AGP 8.7.3 / Gradle 8.9 / Kotlin 2.0.21 / KSP
- Compose BOM 2024.10.01 / Material 3
- Room 2.6.1 / OkHttp 4.12 / kotlinx.serialization
- security-crypto 1.1.0-alpha06
