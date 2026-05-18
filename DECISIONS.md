# DECISIONS

## Default Routine Before Full Routine Builder

For MVP, BeingFlow stores and starts one default routine named `기본 루틴` rather than implementing a full routine builder UI. The default routine is meditation -> work, matching the current product direction while avoiding a larger customization surface.

## Local Journal Fallback

Daily journal/history should work even when OpenAI is disabled or no API key is configured. In that case, the app saves a factual local draft assembled only from completed local transcripts. OpenAI is used only when the user has provided a key and enabled the feature.

## Android Native Validation

The active objective mentions `flutter analyze`, but this repository is a native Android Kotlin/Jetpack Compose project. Flutter validation is not applicable. The validation gates are `./gradlew test` and `./gradlew assembleDebug`.

## No Scope Expansion

The MVP intentionally excludes auth, cloud sync, payments, social features, advanced analytics, production STT, and complex UI polish.
