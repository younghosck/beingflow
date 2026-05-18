# PRODUCT_SPEC

## Product Vision

BeingFlow is an Android-first app for alternating meditation and focused work.

The product is not a productivity dashboard. It is a calm routine companion that helps the user move through a simple loop:

1. meditate
2. briefly record what was observed
3. focus on work
4. briefly record what was done
5. review the day through a factual journal/history

The core value is the pause after each timed segment. The app should make it easy to notice what happened, say it out loud, and continue.

For MVP, BeingFlow should be boring but complete: the flow should work reliably before the app becomes visually polished.

## Target User

The target user is someone who wants a lightweight structure for meditation and focused work without turning their day into a performance dashboard.

They may be:

- studying, coding, reading, writing, or doing other focused work
- practicing meditation before and after work sessions
- trying to remember what they actually observed or did during the day
- more interested in calm continuity than streaks, points, or productivity metrics

The app should support users who want to be consistent, but it should not pressure them to optimize themselves.

## Core User Flow

1. The user chooses an existing routine or creates a simple routine.
2. The user starts the meditation timer.
3. When meditation ends, the app pauses and asks for a short voice note.
4. The user records what thoughts, sensations, or observations appeared.
5. The user saves or intentionally skips the note.
6. The user starts the focus/work timer.
7. When work ends, the app pauses and asks for a short voice note.
8. The user records what they worked on.
9. The user saves or intentionally skips the note.
10. The user reviews today’s history and journal.

MVP may use one default routine, but the product direction should leave room for user-created routines.

## MVP Scope

The MVP should include:

- Android native app
- Kotlin
- Jetpack Compose UI
- A default meditation-work routine
- Ability to start and continue the routine
- Meditation timer
- Focus/work timer
- Voice note after every timed segment
- Local audio storage when recording is possible
- Live transcription during the note flow when available
- Clear pending transcription state when transcript is unavailable
- Today history screen showing notes and statuses
- Daily journal/history screen
- Local persistence for sessions, segments, notes, and journals
- Settings for routine durations
- Basic OpenAI integration only when the user provides an API key and enables it
- Korean-first app UI

The MVP should prioritize:

- reliable flow
- clear next action
- correct local storage
- honest transcription behavior
- simple review of the day

## Non-Goals

BeingFlow should not become:

- a productivity dashboard
- a habit tracker
- a streak app
- a social app
- an analytics-heavy app
- a project management tool
- a task manager
- a calendar replacement
- a gamified focus app

For MVP, avoid:

- complex charts
- scoring systems
- social sharing
- leaderboards
- badges
- deep task hierarchy
- excessive customization
- AI-generated claims not grounded in user notes

## UX Principles

- Calm over motivating.
- Practical over beautiful.
- Minimal over feature-rich.
- The next action should always be obvious.
- The app should pause at the end of each timer and ask for reflection.
- Never auto-skip the voice note step.
- Voice note recording should feel trustworthy.
- Pending transcription should be shown honestly.
- Korean labels should be natural, concise, and low-pressure.
- Avoid productivity-pressure language.
- Avoid dashboard density.
- Do not make users feel judged for skipping or having sparse notes.
- Prefer clear states over clever interactions.

## Data That Must Be Stored Locally

The app must store enough local data to recover the routine and review the day.

Required local data:

- routines
  - routine id
  - routine name
  - ordered segment definitions
  - default durations
- routine sessions
  - session id
  - routine id
  - started time
  - completed time if completed
  - status
- routine segments
  - segment id
  - session id
  - order
  - type: meditation or work
  - planned duration
  - started time
  - expected end time
  - ended time if ended
  - status
  - meditation type if applicable
  - work label if applicable
- voice notes
  - note id
  - session id
  - segment id
  - note type
  - created time
  - local audio path if available
  - transcript if available
  - transcription status
  - transcription source
  - whether transcript was edited
- daily journal/history
  - date
  - generated or assembled time
  - content
  - source note ids
  - status
- settings
  - meditation duration
  - work duration
  - journal time if scheduled
  - OpenAI enabled flags
  - model names if configurable
  - API key stored securely when provided

Audio files should be stored in app-private storage. The app should not log note text, sensitive paths, or API keys.

## Acceptance Criteria

An MVP build is acceptable when:

- The app installs and launches on an Android device.
- The user can start a routine from the app.
- The routine contains at least meditation -> work -> meditation.
- The timer shows the current segment and remaining time.
- The timer can recover remaining time from persisted timestamps after pause/resume.
- When a timer ends, the app requires a voice note step before continuing.
- The user can record, stop, retry, save, or skip a voice note.
- If live transcription succeeds, the transcript is shown and editable.
- If live transcription fails or is empty, the note can still be saved with pending transcription status.
- Audio files are stored locally when recording succeeds.
- Today history shows the day’s notes, note type, transcript status, audio existence, and transcript preview.
- The journal/history screen shows a factual daily journal or a clear message when there are no usable notes.
- OpenAI calls happen only after the user enters an API key and enables the relevant feature.
- OpenAI errors do not delete local notes.
- Settings allow changing routine durations.
- The UI is Korean-first.
- The app avoids productivity-dashboard, habit-tracker, and analytics-heavy patterns.
- The repository includes README, product spec, roadmap, and implementation notes.
- Unit tests cover routine state transitions and journal prompt behavior.
- `./gradlew test` and `./gradlew assembleDebug` pass, or any hard environment blocker is documented precisely.
