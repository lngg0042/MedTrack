# MedTrack Pro - Assignment 3

**Student:** Ng Li Xian (33986010)  
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

### Band A – Core Refactoring (50%)
- **Room Database**: 5 tables (Patient, Medication, Symptom, MedCoachTips, TakenStatus)
- **CSV Seeding**: First-launch seeding via SharedPreferences flag (`db_seeded`)
- **Login System**: Account claiming (PatientID + Phone → set password), DB-backed login, session persistence
- **MVVM Architecture**: ViewModel → Repository → DAO for all screens, StateFlow reactivity, coroutines for all DB ops
- **Settings Screen**: Shows name, phone, patient ID; logout clears session and back-stack

### Band B – Feature Expansion (+25%)
- **MedCoach Screen – Drug Info**: OpenFDA API via Retrofit, 5 fields displayed, patient medication dropdown, error handling
- **MedCoach Screen – GenAI Tips**: Gemini API, personalised tips stored in Room, "Show All Tips" dialog
- **Improved GenAI (F)**: Prompt includes patient name, medication list, and recent symptoms for personalisation

### Band C – HD Extensions (+25%)
- **Taken Toggle Persistence (G)**: TakenStatus table in Room, daily reset based on stored date comparison
- **Clinician Dashboard (H)**: Access key `dollar-entry-apples`, 4 aggregate stats, GenAI pattern insights
- **Original Feature (I)**: [Add your Band I feature description here]

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

## AI Usage Declaration
This project uses Gemini AI (via REST API) for:
1. Patient-personalised medication adherence tips (MedCoach screen)
2. Clinician data pattern analysis (Clinician Dashboard)

Gemini was also used during development for code assistance (declared per Monash GenAI policy).
