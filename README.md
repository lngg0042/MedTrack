# MedTrack Pro
An Android app that helps patients keep track of their medications and symptoms, with AI-powered adherence tips and a clinician dashboard. Built with Kotlin, Jetpack Compose and Room.

**Student:** Ng Li Xian
**Package:** com.ng.s33986010.medtrack

## Setup Instructions

### 1. Add Your Gemini API Key
Open `local.properties` in the project root (Android Studio creates it; it is not committed to Git) and add:
```
GEMINI_API_KEY=your_key_here
```
Get a key from https://aistudio.google.com, then Sync Gradle.

### 2. Build & Run
Open in Android Studio → Sync Gradle → Run on device/emulator (API 26+)

---

## Features Implemented

### Core
- **Room Database**: 5 tables (Patient, Medication, Symptom, MedCoachTips, TakenStatus)
- **CSV Seeding**: First-launch seeding via SharedPreferences flag (`db_seeded`)
- **Login System**: Account claiming (PatientID + Phone → set password), DB-backed login, session persistence
- **MVVM Architecture**: ViewModel → Repository → DAO for all screens, StateFlow reactivity, coroutines for all DB ops
- **Settings Screen**: Shows name, phone, patient ID; logout clears session and back-stack

### MedCoach
- **Drug info**: looks up the patient's medications on the OpenFDA API (Retrofit) and shows 5 key fields, with error handling
- **AI tips**: personalised adherence tips from Gemini, saved in Room, with a "Show All Tips" dialog
- **Smarter prompts**: tips take into account the patient's name, medication list and recent symptoms

### Extensions
- **Taken Toggle Persistence (G)**: TakenStatus table in Room, daily reset based on stored date comparison
- **Clinician Dashboard (H)**: Access key `dollar-entry-apples`, 4 aggregate stats, GenAI pattern insights
  
---

## Tech stack

| Area | Library |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | ViewModel, StateFlow, Kotlin Coroutines |
| Local data | Room (with KSP) |
| Networking | Retrofit, OkHttp, Gson |
| APIs | [OpenFDA](https://open.fda.gov/), [Google Gemini](https://aistudio.google.com) |

Min SDK 26 · Target SDK 36

---

## Architecture Overview

```
UI Composables (View)
    ↓ actions / events
ViewModels (StateFlow/LiveData)
    ↓ suspend calls
Repository (single source of truth)
    ↓
DAOs ←→ Room Database
    +
RetrofitClient ←→ OpenFDA API
                ←→ Gemini API
```

### Project structure
```
app/src/main/java/com/ng/s33986010/medtrack/
├── MainActivity.kt
├── data/
│   ├── dao/           # Room DAOs
│   ├── database/      # MedTrackDatabase
│   └── repository/    # MedTrackRepository
├── model/             # Room entities
├── network/           # API request/response models
├── ui/
│   ├── screens/       # Auth, Home, Symptoms, Symptom Trend, MedCoach, Settings & Clinician, Navigation
│   └── theme/
└── viewmodel/         # ViewModels
```

---

## AI Usage Declaration
This project uses Gemini AI (via REST API) for:
1. Patient-personalised medication adherence tips (MedCoach screen)
2. Clinician data pattern analysis (Clinician Dashboard)

Gemini was also used during development for code assistance (declared per Monash GenAI policy).
