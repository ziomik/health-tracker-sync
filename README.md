# Health Tracker Sync - Android Companion App

App Android companion per sincronizzare dati da Health Connect a Health Tracker server.

## Features

- ✅ Scan QR code per setup automatico
- ✅ Integrazione Health Connect
- ✅ Sync automatica in background ogni 3 ore
- ✅ Notifiche push per stato sync
- ✅ Supporto WiFi + dati mobili
- ✅ Ottimizzata batteria

## Setup

1. Installa app da APK
2. Concedi permessi Health Connect
3. Scansiona QR code da webapp (Impostazioni → Integrazioni)
4. Sync automatica attiva!

## Dati Sincronizzati

- 💓 Frequenza cardiaca (HR)
- 🫁 Saturazione ossigeno (SpO2)
- 🚶 Passi, distanza, calorie
- 😴 Sonno (fasi + durata)
- 🩸 Pressione sanguigna
- 🌡️ Temperatura corporea
- ⚖️ Peso
- 🏃 Attività fisica

## Tech Stack

- Kotlin
- Jetpack Compose
- Health Connect SDK
- WorkManager (background sync)
- Retrofit (API calls)
- ZXing (QR scanner)

## Build

```bash
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

## Privacy

- ✅ Dati non passano per servizi terzi
- ✅ Comunicazione diretta con server Health Tracker
- ✅ Token API sicuri
- ✅ HTTPS supportato (configurable)

---

**Sviluppato per Health Tracker v2.0**
