# ARCHITECTURE

## Platform

- Android-first native app
- Kotlin
- Jetpack Compose
- Room
- WorkManager
- Android SpeechRecognizer
- MediaRecorder
- OkHttp for OpenAI API calls

## Layers

### UI

- `MainActivity.kt`
- Compose screens for Today Routine, Timer, Voice Note, Today Records, Daily Diary, and Settings.

### ViewModel

- `MainViewModel.kt`
- Coordinates UI state, routine actions, note saving, settings saving, and diary generation.

### Domain

- `domain/Models.kt`
- Routine settings, segment types, statuses, state machine, and default segment planning.

### Data

- `data/Entities.kt`
- `data/Daos.kt`
- `data/RoutineRepository.kt`
- `data/BeingFlowDatabase.kt`

Room stores routine definitions, sessions, segments, voice notes, and daily diaries.

### Timer

- `timer/TimerController.kt`
- `timer/TimerNotifier.kt`

Timers are based on persisted expected end timestamps and wall-clock recomputation.

### Audio and Transcription

- `audio/AudioRecorder.kt`
- `transcription/TranscriptionProvider.kt`

Audio is stored in app-private files. Android SpeechRecognizer is used only for live transcription during the note flow. OpenAI audio transcription can retry pending audio when enabled by the user.

### Diary

- `diary/DiaryPromptBuilder.kt`
- `diary/OpenAiDiaryGenerator.kt`

OpenAI diary generation uses provided transcripts only. Local journal fallback creates a factual draft without network calls.

### Background Work

- `worker/DailyDiaryWorker.kt`

Schedules daily journal generation at the configured local time.

## Persistence

- Room DB: structured product data
- app-private files: voice note audio
- SharedPreferences: routine and OpenAI settings
- EncryptedSharedPreferences preferred for API key storage

