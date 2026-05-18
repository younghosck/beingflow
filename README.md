# being flow

`being flow`는 명상-작업 루틴을 위한 Android MVP 앱입니다. 핵심은 타이머가 끝날 때마다 다음 단계로 자동으로 넘어가지 않고, 사용자가 짧은 음성 기록을 남기도록 멈추는 것입니다. 하루가 끝나면 그날의 전사 기록만 바탕으로 사실적인 한국어 일기를 생성합니다.

## 주요 흐름

1. 명상 5분
2. 명상 중 관찰한 생각, 감각, 마음의 움직임 음성 기록
3. 작업 40분
4. 방금 한 일 음성 기록
5. 설정된 시간 또는 수동 버튼으로 오늘 일기 생성

## 기술 스택

- Kotlin
- Jetpack Compose + Material 3
- Room
- WorkManager
- Android SpeechRecognizer
- MediaRecorder app-private audio storage
- OkHttp 기반 OpenAI Responses API / Audio Transcriptions API 연동
- minSdk 26, compileSdk 36

## 빌드와 실행

```bash
./gradlew test
./gradlew assembleDebug
```

Android Studio에서 열 때는 이 저장소 루트(`/Users/05sck/dev/beingflow`)를 열면 됩니다. 디버그 APK는 빌드 성공 후 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

## 권한

앱은 다음 권한을 사용합니다.

- `RECORD_AUDIO`: 음성 기록 저장과 실시간 전사
- `POST_NOTIFICATIONS`: Android 13 이상에서 타이머 종료 알림
- `VIBRATE`: 타이머 종료 진동
- `INTERNET`: 사용자가 OpenAI 기능을 켠 경우에만 API 호출

## OpenAI API 키

설정 화면에서 OpenAI API 키를 입력합니다. 키는 코드에 하드코딩하지 않으며 로그에 남기지 않습니다. AndroidX Security의 `EncryptedSharedPreferences`를 우선 사용하고, 기기 환경에서 초기화가 실패하면 앱 전용 SharedPreferences fallback을 사용합니다.

OpenAI 전송은 다음 조건을 모두 만족할 때만 발생합니다.

- 사용자가 API 키를 입력함
- 설정에서 OpenAI 일기 생성 또는 OpenAI 음성 전사를 켬
- 수동 일기 생성 버튼을 누르거나 WorkManager 일일 작업이 실행됨

기본 모델값:

- 일기 생성: `gpt-4o-mini`
- 오디오 전사: `gpt-4o-mini-transcribe`

두 모델명은 설정 화면에서 수정할 수 있습니다.

## 로컬 저장 방식

- 음성 파일은 앱 내부 저장소의 `voice-notes` 디렉터리에 저장됩니다.
- 세션, 세그먼트, 음성 기록, 일기는 Room DB에 저장됩니다.
- 전사가 실패하거나 비어 있으면 기록은 삭제하지 않고 `전사 대기 중` 상태로 남깁니다.
- 일기는 제공된 전사 텍스트만 사용하며 기록에 없는 사실을 만들지 않도록 프롬프트를 구성합니다.

## 일일 일기 생성

WorkManager가 설정된 현지 시간, 기본 22:30에 일일 작업을 예약합니다. 작업은 오늘의 기록을 조회하고, OpenAI 전사가 켜져 있으면 pending 오디오 전사를 먼저 시도한 뒤, 사용 가능한 전사 텍스트로 사실적인 한국어 일기를 생성합니다.

기록이 없거나 완료된 전사가 없으면 `"오늘 기록이 없어 일기를 생성하지 않았습니다."` 상태를 저장합니다. 네트워크 또는 API 오류가 나면 실패 상태를 저장하고 재시도할 수 있게 둡니다.

## Android SpeechRecognizer 한계

Android `SpeechRecognizer`는 저장된 음성 파일을 안정적으로 사후 전사하는 도구가 아니라, 녹음 흐름 중 실시간 음성을 인식하는 API입니다. 이 MVP는 그 한계를 숨기지 않습니다.

- 음성 기록 화면은 가능한 경우 로컬 오디오 파일을 저장합니다.
- 동시에 SpeechRecognizer로 실시간 전사를 시도합니다.
- 실시간 전사가 실패하면 오디오 파일과 `전사 대기 중` 상태를 남깁니다.
- OpenAI 음성 전사를 켜고 API 키를 입력한 경우, 수동 일기 생성 또는 일일 작업에서 pending 오디오 전사를 재시도합니다.

## 현재 화면

- 오늘 루틴
- 타이머
- 음성 기록
- 오늘 기록
- 오늘 일기
- 설정

## 알려진 제한

- MVP는 오늘 날짜 중심이며 과거 날짜 선택 UI는 단순화되어 있습니다.
- 타이머는 앱 데이터에 종료 시각을 저장하고 복귀 시 재계산하지만, 장시간 백그라운드 타이머 foreground service UI는 아직 없습니다.
- Android SpeechRecognizer 품질은 기기, 언어 설정, 네트워크 상태에 따라 다릅니다.
- OpenAI API 오류 상세는 사용자에게 간단한 한국어 메시지로만 표시합니다.
