# Health Tracker Sync - Build Instructions

## Prerequisiti

1. **Android Studio** (Giraffe o successivi)
   - Download: https://developer.android.com/studio

2. **JDK 17**
   ```bash
   sudo apt install openjdk-17-jdk
   ```

3. **Android SDK** (installato tramite Android Studio)

## Build Steps

### Opzione 1: Android Studio (consigliata)

1. Apri Android Studio
2. File → Open → Seleziona `/home/ziomik/clawd/health-tracker/android-companion`
3. Attendi sync Gradle (può richiedere 5-10 min prima volta)
4. Build → Generate Signed Bundle / APK
5. Seleziona APK
6. Crea nuovo keystore o usa esistente
7. Build → Release
8. APK generato in: `app/build/outputs/apk/release/app-release.apk`

### Opzione 2: Command Line

```bash
cd /home/ziomik/clawd/health-tracker/android-companion

# Build debug (per testing)
./gradlew assembleDebug

# Build release (richiede keystore)
./gradlew assembleRelease

# APK output
ls -lh app/build/outputs/apk/release/
```

## Keystore Setup (Prima volta)

```bash
keytool -genkey -v -keystore health-sync-release.keystore \
  -alias health-sync -keyalg RSA -keysize 2048 -validity 10000

# Inserisci:
# - Password keystore
# - Nome, Organizzazione, etc.
# - Password key (può essere uguale a keystore)
```

Poi crea `app/keystore.properties`:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=health-sync
storeFile=../health-sync-release.keystore
```

## Installazione APK

### Via USB (ADB)

```bash
# Abilita "Developer Options" e "USB Debugging" su telefono
adb install app/build/outputs/apk/release/app-release.apk
```

### Via File Transfer

1. Copia APK su telefono (USB, email, cloud)
2. Apri APK da file manager
3. Concedi permesso "Installa app sconosciute"
4. Installa

## Testing

### Emulatore Android Studio

1. Tools → Device Manager
2. Create Virtual Device
3. Seleziona Pixel 6 (o simile, API 33+)
4. Download system image se necessario
5. Run app

**NOTA:** Health Connect potrebbe non funzionare su emulatore. Usare device fisico per test completi.

### Device Fisico

1. Abilita Developer Options:
   - Settings → About Phone
   - Tap "Build Number" 7 volte

2. Abilita USB Debugging:
   - Settings → Developer Options → USB Debugging

3. Connetti via USB e autorizza computer

4. Run da Android Studio o `./gradlew installDebug`

## Troubleshooting

### "SDK not found"

```bash
# Installa Android SDK via Android Studio
# Oppure:
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### "Gradle sync failed"

1. File → Invalidate Caches → Invalidate and Restart
2. Build → Clean Project
3. Build → Rebuild Project

### "Health Connect not installed"

App richiede Android 8.0+ e Health Connect app installata.

Install Health Connect:
https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata

## Distribution

### Google Play (Future)

1. Create signed bundle (AAB):
   ```bash
   ./gradlew bundleRelease
   ```

2. Upload a Google Play Console
   https://play.google.com/console

### Direct APK Distribution

APK firmato può essere distribuito direttamente via:
- Email
- Cloud storage (Google Drive, Dropbox)
- Self-hosted download link

**IMPORTANTE:** Utenti devono abilitare "Installa da fonti sconosciute"

## Next Steps

Dopo build e install:

1. Apri app su telefono
2. Concedi permessi Health Connect quando richiesto
3. Vai su webapp Health Tracker → Settings → Integrations
4. Click "Genera QR Code"
5. Scansiona QR dall'app Android
6. Sync automatica attiva!

---

**Per domande o problemi:** Check logs in Android Studio Logcat
