# MVP_ACCEPTANCE

## Required Flow

- User can start the default routine.
- Routine runs meditation -> voice note -> work -> voice note.
- Timers show remaining time and persist expected end timestamps.
- Each timer completion pauses the routine until the voice note is saved or skipped.
- Voice notes store transcript status and audio path when recording succeeds.
- Today Records shows the day’s notes and statuses.
- Daily Diary/History shows either an OpenAI-generated factual diary or a local factual draft.

## Required Validation

- `./gradlew test`
- `./gradlew assembleDebug`

## Non-Goals

- Flutter implementation
- auth
- cloud sync
- payments
- social features
- advanced analytics
- production STT
- complex UI polish
