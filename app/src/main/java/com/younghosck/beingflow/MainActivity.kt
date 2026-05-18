package com.younghosck.beingflow

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.younghosck.beingflow.data.RoutineSegmentEntity
import com.younghosck.beingflow.data.VoiceNoteEntity
import com.younghosck.beingflow.domain.MeditationType
import com.younghosck.beingflow.domain.RoutineSettings
import com.younghosck.beingflow.domain.SegmentStatus
import com.younghosck.beingflow.domain.SegmentType
import com.younghosck.beingflow.domain.TranscriptionSource
import com.younghosck.beingflow.domain.TranscriptionStatus
import com.younghosck.beingflow.timer.TimerController
import com.younghosck.beingflow.timer.formatMinutesSeconds
import com.younghosck.beingflow.ui.MainUiState
import com.younghosck.beingflow.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as BeingFlowApp).container)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        permissionLauncher.launch(permissions)
        setContent {
            BeingFlowTheme {
                BeingFlowAppScreen(viewModel)
            }
        }
    }
}

enum class AppTab(val label: String) {
    TODAY("오늘"),
    TIMER("타이머"),
    RECORDS("기록"),
    DIARY("일기"),
    SETTINGS("설정")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeingFlowAppScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(AppTab.TODAY) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(state.activeSegment?.id, state.activeSegment?.status) {
        if (state.activeSegment?.status == SegmentStatus.NOTE_REQUIRED) tab = AppTab.TIMER
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("being flow") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                AppTab.values().forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        label = { Text(it.label) },
                        icon = {}
                    )
                }
            }
        }
    ) { padding ->
        Surface(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                AppTab.TODAY -> TodayRoutineScreen(state, viewModel, onTimer = { tab = AppTab.TIMER })
                AppTab.TIMER -> TimerOrVoiceScreen(state, viewModel)
                AppTab.RECORDS -> TodayRecordsScreen(state.todayNotes)
                AppTab.DIARY -> DailyDiaryScreen(state, viewModel)
                AppTab.SETTINGS -> SettingsScreen(state.settings, viewModel)
            }
        }
    }
}

@Composable
fun TodayRoutineScreen(state: MainUiState, viewModel: MainViewModel, onTimer: () -> Unit) {
    ScreenColumn {
        Text("오늘 루틴", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("명상 ${state.settings.meditationSeconds / 60}분 → 작업 ${state.settings.workSeconds / 60}분 → 명상 ${state.settings.finalMeditationSeconds / 60}분")
        Spacer(Modifier.height(12.dp))
        if (state.activeSegment == null) {
            Button(onClick = { viewModel.startSession(); onTimer() }) { Text("새 루틴 시작") }
        } else {
            Button(onClick = onTimer) { Text("현재 단계로 이동") }
        }
        Spacer(Modifier.height(16.dp))
        Text("오늘 완료된 기록", fontWeight = FontWeight.Bold)
        if (state.todayNotes.isEmpty()) {
            Text("아직 저장된 음성 기록이 없습니다.")
        } else {
            state.todayNotes.take(5).forEach { NoteCard(it) }
        }
        Button(onClick = { viewModel.generateDiary() }, enabled = !state.isGeneratingDiary) {
            Text(if (state.diary?.status?.name == "FAILED") "오늘 일기 다시 생성" else "오늘 일기 생성")
        }
    }
}

@Composable
fun TimerOrVoiceScreen(state: MainUiState, viewModel: MainViewModel) {
    val segment = state.activeSegment
    if (segment == null) {
        ScreenColumn { Text("진행 중인 루틴이 없습니다."); Button(onClick = viewModel::startSession) { Text("새 루틴 시작") } }
        return
    }
    if (segment.status == SegmentStatus.NOTE_REQUIRED) {
        VoiceNoteScreen(segment, viewModel)
    } else {
        TimerScreen(segment, viewModel)
    }
}

@Composable
fun TimerScreen(segment: RoutineSegmentEntity, viewModel: MainViewModel) {
    var remaining by remember(segment.id) { mutableStateOf(segment.plannedDurationSeconds.toLong()) }
    var taskLabel by remember(segment.id) { mutableStateOf(segment.taskLabel.orEmpty()) }
    var meditationType by remember(segment.id) { mutableStateOf(segment.meditationType ?: MeditationType.SITTING) }

    LaunchedEffect(segment.id, segment.expectedEndAt) {
        TimerController.observe(segment).collect {
            remaining = it.remainingSeconds
            if (it.isFinished) viewModel.timerFinished(segment)
        }
    }

    ScreenColumn {
        Text(if (segment.type == SegmentType.WORK) "작업" else "명상", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(remaining.formatMinutesSeconds(), style = MaterialTheme.typography.displayMedium)
        Text("다음 행동: 시간이 끝나면 짧은 음성 기록을 남깁니다.")
        if (segment.type == SegmentType.MEDITATION) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = meditationType == MeditationType.SITTING,
                    onClick = { meditationType = MeditationType.SITTING; viewModel.configureSegment(segment.id, meditationType, null) },
                    label = { Text("좌선") }
                )
                FilterChip(
                    selected = meditationType == MeditationType.WALKING,
                    onClick = { meditationType = MeditationType.WALKING; viewModel.configureSegment(segment.id, meditationType, null) },
                    label = { Text("걷기 명상") }
                )
            }
        } else {
            OutlinedTextField(
                value = taskLabel,
                onValueChange = { taskLabel = it; viewModel.configureSegment(segment.id, null, it) },
                label = { Text("작업 이름") },
                placeholder = { Text("공부, 독서, 코딩") }
            )
        }
    }
}

@Composable
fun VoiceNoteScreen(segment: RoutineSegmentEntity, viewModel: MainViewModel) {
    val context = LocalContext.current
    val container = (context.applicationContext as BeingFlowApp).container
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var transcript by remember { mutableStateOf("") }
    var transcriptStatus by remember { mutableStateOf(TranscriptionStatus.NONE) }
    var source by remember { mutableStateOf(TranscriptionSource.NONE) }

    ScreenColumn {
        Text("음성 기록", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(if (segment.type == SegmentType.WORK) "방금 어떤 일을 했는지 짧게 말해주세요." else "방금 명상 중 관찰한 생각, 감각, 마음의 움직임을 짧게 말해주세요.")
        OutlinedTextField(
            value = transcript,
            onValueChange = { transcript = it; source = TranscriptionSource.MANUAL },
            label = { Text(if (transcriptStatus == TranscriptionStatus.PENDING) "전사 대기 중" else "전사 내용") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    audioFile = container.audioRecorder.start()
                    isRecording = true
                    transcriptStatus = TranscriptionStatus.PENDING
                    source = TranscriptionSource.ANDROID_SPEECH_RECOGNIZER
                    scope.launch {
                        container.liveTranscriptionProvider.start().collect {
                            if (!it.text.isNullOrBlank()) transcript = it.text
                            transcriptStatus = it.status
                            source = it.source
                        }
                    }
                },
                enabled = !isRecording
            ) { Text("녹음") }
            Button(
                onClick = {
                    audioFile = container.audioRecorder.stop() ?: audioFile
                    isRecording = false
                    if (transcript.isBlank()) transcriptStatus = TranscriptionStatus.PENDING
                },
                enabled = isRecording
            ) { Text("정지") }
            TextButton(onClick = {
                transcript = ""
                transcriptStatus = TranscriptionStatus.NONE
                source = TranscriptionSource.NONE
                audioFile = null
            }) { Text("다시") }
        }
        Text("상태: ${if (transcript.isBlank()) "전사 대기 중" else transcriptStatus.korean()}")
        Button(
            onClick = {
                val status = if (transcript.isBlank()) TranscriptionStatus.PENDING else TranscriptionStatus.COMPLETED
                val finalSource = if (source == TranscriptionSource.NONE && transcript.isNotBlank()) TranscriptionSource.MANUAL else source
                viewModel.saveNote(segment, audioFile?.absolutePath, transcript, source == TranscriptionSource.MANUAL, finalSource, status)
            }
        ) { Text("저장하고 계속") }
        TextButton(onClick = { viewModel.skipNote(segment) }) { Text("건너뛰기") }
    }
}

@Composable
fun TodayRecordsScreen(notes: List<VoiceNoteEntity>) {
    ScreenColumn {
        Text("오늘 기록", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (notes.isEmpty()) Text("오늘 저장된 기록이 없습니다.")
        notes.forEach { NoteCard(it, full = true) }
    }
}

@Composable
fun DailyDiaryScreen(state: MainUiState, viewModel: MainViewModel) {
    ScreenColumn {
        Text("오늘 일기", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = { viewModel.generateDiary() }, enabled = !state.isGeneratingDiary) {
            Text("오늘 일기 생성")
        }
        Card(Modifier.fillMaxWidth()) {
            Text(
                state.diary?.content ?: "아직 생성된 일기가 없습니다.",
                Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun SettingsScreen(settings: RoutineSettings, viewModel: MainViewModel) {
    var meditation by remember(settings) { mutableStateOf((settings.meditationSeconds / 60).toString()) }
    var work by remember(settings) { mutableStateOf((settings.workSeconds / 60).toString()) }
    var finalMeditation by remember(settings) { mutableStateOf((settings.finalMeditationSeconds / 60).toString()) }
    var diaryTime by remember(settings) { mutableStateOf("%02d:%02d".format(settings.diaryHour, settings.diaryMinute)) }
    var apiKey by remember { mutableStateOf("") }
    var diaryEnabled by remember(settings) { mutableStateOf(settings.openAiDiaryEnabled) }
    var transcriptionEnabled by remember(settings) { mutableStateOf(settings.openAiTranscriptionEnabled) }
    var diaryModel by remember(settings) { mutableStateOf(settings.diaryModel) }
    var transcriptionModel by remember(settings) { mutableStateOf(settings.transcriptionModel) }
    var meditationType by remember(settings) { mutableStateOf(settings.defaultMeditationType) }

    ScreenColumn {
        Text("설정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        NumberField("명상 시간(분)", meditation) { meditation = it }
        NumberField("작업 시간(분)", work) { work = it }
        NumberField("마지막 명상 시간(분)", finalMeditation) { finalMeditation = it }
        OutlinedTextField(value = diaryTime, onValueChange = { diaryTime = it }, label = { Text("일기 생성 시간 HH:mm") })
        Text("기본 명상 방식")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = meditationType == MeditationType.SITTING,
                onClick = { meditationType = MeditationType.SITTING },
                label = { Text("좌선") }
            )
            FilterChip(
                selected = meditationType == MeditationType.WALKING,
                onClick = { meditationType = MeditationType.WALKING },
                label = { Text("걷기 명상") }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("OpenAI 일기 생성"); Switch(diaryEnabled, { diaryEnabled = it }) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("OpenAI 음성 전사"); Switch(transcriptionEnabled, { transcriptionEnabled = it }) }
        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("OpenAI API 키") }, placeholder = { Text("저장 후 표시하지 않습니다") })
        OutlinedTextField(value = diaryModel, onValueChange = { diaryModel = it }, label = { Text("일기 모델") })
        OutlinedTextField(value = transcriptionModel, onValueChange = { transcriptionModel = it }, label = { Text("전사 모델") })
        Text("개인 정보: 음성 파일과 전사 텍스트는 기본적으로 앱 내부 저장소와 로컬 DB에 저장됩니다. OpenAI 기능을 켜고 API 키를 입력한 경우에만 기록이 OpenAI로 전송됩니다.")
        Button(onClick = {
            val parts = diaryTime.split(":")
            viewModel.saveSettings(
                RoutineSettings(
                    meditationSeconds = (meditation.toIntOrNull() ?: 5) * 60,
                    workSeconds = (work.toIntOrNull() ?: 40) * 60,
                    finalMeditationSeconds = (finalMeditation.toIntOrNull() ?: 5) * 60,
                    diaryHour = parts.getOrNull(0)?.toIntOrNull() ?: 22,
                    diaryMinute = parts.getOrNull(1)?.toIntOrNull() ?: 30,
                    defaultMeditationType = meditationType,
                    openAiDiaryEnabled = diaryEnabled,
                    openAiTranscriptionEnabled = transcriptionEnabled,
                    diaryModel = diaryModel,
                    transcriptionModel = transcriptionModel
                ),
                apiKey
            )
        }) { Text("설정 저장") }
    }
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) })
}

@Composable
fun NoteCard(note: VoiceNoteEntity, full: Boolean = false) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (note.noteType.name == "WORK_REPORT") "작업 기록" else "명상 기록", fontWeight = FontWeight.Bold)
            Text("${formatTime(note.createdAt)} · ${note.transcriptionStatus.korean()} · 오디오 ${if (note.audioPath == null) "없음" else "있음"}")
            Text(note.transcript ?: "전사 대기 중", maxLines = if (full) Int.MAX_VALUE else 2)
        }
    }
}

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun BeingFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

fun TranscriptionStatus.korean(): String = when (this) {
    TranscriptionStatus.NONE -> "전사 없음"
    TranscriptionStatus.PENDING -> "전사 대기 중"
    TranscriptionStatus.COMPLETED -> "전사 완료"
    TranscriptionStatus.FAILED -> "전사 실패"
}

fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
