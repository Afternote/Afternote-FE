package com.afternote.feature.home.presentation.receiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.ui.icon.AfternoteSourceIcon
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.home.presentation.R
import com.afternote.feature.home.presentation.receiver.model.MindRecordSummary
import com.afternote.feature.home.presentation.receiver.model.ReceiverDownloadState
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState
import com.afternote.feature.home.presentation.receiver.model.SenderMessage
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.receiver.presentation.reporting.ReceiverFailureStage
import com.afternote.feature.receiver.presentation.reporting.recordReceiverFailure
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 홈 화면 ViewModel.
 *
 * 한 마디·각 섹션 카운트·애프터노트 아이콘 목록을 한 번에 모아 단일 [ReceiverHomeUiState]로 노출하고,
 * 모든 기록 내려받기 다이얼로그/요청 상태도 동일 객체에 합친다.
 */
@HiltViewModel
class ReceiverHomeViewModel
    @Inject
    constructor(
        private val receiverRepository: ReceiverRepository,
        private val mindRecordReceiverRepository: MindRecordReceiverRepository,
        private val receiverTimeLetterRepository: ReceiverTimeLetterRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ReceiverHomeUiState>(ReceiverHomeUiState.Loading)
        val uiState: StateFlow<ReceiverHomeUiState> = _uiState.asStateFlow()

        /** 진행 중인 홈 로드 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — Job 가드만으로는 init 로드가 빨리 끝난 뒤 도착한 첫
         * resume 이 순차 재조회를 건다. VM 필드인 이유는
         * [com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel] 과
         * 동일 — 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        init {
            loadHome()
        }

        /**
         * 다른 화면에서 홈으로 복귀했을 때의 자동 갱신 (#701).
         *
         * [ReceiverHomeEvent.Retry] 와 두 가지가 다르다 — 로딩을 방출하지 않고, 실패해도 보고 있던
         * 화면을 유지한다(부분 실패도 구멍 난 새 화면 대신 완결된 기존 화면을 남긴다).
         * 첫 ON_RESUME(진입 자체)은 [isFirstResume] 로 스킵하고, 그 이후의 resume 이 실행 중인
         * 로드와 겹치면 진행 중인 Job 으로 건너뛴다.
         */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true) return
            loadHome(showsLoading = false, keepsStateOnFailure = true)
        }

        fun onEvent(event: ReceiverHomeEvent) {
            when (event) {
                ReceiverHomeEvent.Retry -> loadHome()
                ReceiverHomeEvent.RequestDownload -> updateDownload(ReceiverDownloadState.Confirming)
                ReceiverHomeEvent.DismissDownload -> updateDownload(ReceiverDownloadState.Idle)
                ReceiverHomeEvent.ConfirmDownload -> downloadAll()
                ReceiverHomeEvent.ConsumeDownloadResult -> updateDownload(ReceiverDownloadState.Idle)
            }
        }

        private fun loadHome(
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            loadJob?.cancel()
            if (showsLoading) {
                _uiState.value = ReceiverHomeUiState.Loading
            }
            loadJob =
                viewModelScope.launch {
                    loadHomeInternal(keepsStateOnFailure)
                }
        }

        private suspend fun loadHomeInternal(keepsStateOnFailure: Boolean) {
            coroutineScope {
                val afternotes = async { receiverRepository.getReceivedAfterNotes() }
                val mindRecords = async { mindRecordReceiverRepository.getAll() }
                val timeLetters =
                    async {
                        runCatchingCancellable {
                            receiverTimeLetterRepository.getReceivedTimeLetters()
                        }
                    }
                val message = async { receiverRepository.loadSenderMessage() }
                val afternotesRes = afternotes.await()
                val mindRecordsRes = mindRecords.await()
                val timeLettersRes = timeLetters.await()
                val messageRes = message.await()

                val failedSources =
                    buildList {
                        if (afternotesRes.isFailure) add("afternotes")
                        if (mindRecordsRes.isFailure) add("mind_records")
                        if (timeLettersRes.isFailure) add("time_letters")
                        if (messageRes.isFailure) add("sender_message")
                    }
                val firstFailure =
                    afternotesRes.exceptionOrNull()
                        ?: mindRecordsRes.exceptionOrNull()
                        ?: timeLettersRes.exceptionOrNull()
                        ?: messageRes.exceptionOrNull()

                if (firstFailure != null) {
                    // 모든 호출이 실패한 경우만 Error. 일부 실패는 fallback 으로 진행.
                    if (failedSources.size == HOME_REQUEST_COUNT) {
                        // 화면을 유지하는 자동 갱신 실패도 기록한다 — 콘솔이 유일한 관측 지점이다.
                        errorReporter.recordReceiverFailure(ReceiverFailureStage.RECEIVER_HOME_LOAD, firstFailure)
                        _uiState.update { current ->
                            if (keepsStateOnFailure && current is ReceiverHomeUiState.Success) {
                                // 자동 갱신 실패: 잘 보고 있던 홈을 에러 화면으로 대체하지 않는다.
                                current
                            } else {
                                ReceiverHomeUiState.Error(firstFailure)
                            }
                        }
                        return@coroutineScope
                    }
                    // 일부 실패는 0·빈 값으로 덮여 화면에도 콘솔에도 흔적이 남지 않던 구간 — 여기서만 기록한다.
                    errorReporter.recordReceiverFailure(
                        stage = ReceiverFailureStage.RECEIVER_HOME_PARTIAL_LOAD,
                        throwable = firstFailure,
                        failedSources = failedSources,
                    )
                    if (keepsStateOnFailure && _uiState.value is ReceiverHomeUiState.Success) {
                        // 자동 갱신의 부분 실패: 성공한 소스만 반영하면 실패한 섹션이 null 로 꺼져
                        // 잘 보이던 카운트가 이유 없이 사라진다 — 완결된 기존 화면을 그대로 둔다.
                        return@coroutineScope
                    }
                }

                val afternotesResult = afternotesRes.getOrNull()
                val mindRecordsSummary = mindRecordsRes.getOrNull()?.toHomeSummary()
                val timeLettersCount = timeLettersRes.getOrNull()?.totalCount
                val senderMessageInfo = messageRes.getOrNull()
                // senderName / senderMessage 둘 다 blank 가드 — sender 가 이름·메시지 미입력 케이스 대응.
                // ".orEmpty()" 만으로는 공백("  ") 통과해 "故 님이 남기신 기록" / "님의 한 마디" UI 깨짐.
                val senderName = senderMessageInfo?.senderName?.takeIf { it.isNotBlank() }.orEmpty()
                val senderMessageBody = senderMessageInfo?.message?.takeIf { it.isNotBlank() }

                _uiState.update { current ->
                    ReceiverHomeUiState.Success(
                        senderName = senderName,
                        senderMessage =
                            senderMessageBody?.let {
                                // orEmpty(): null 이면 "" — createdAt 미제공(구버전 서버) 시 날짜 슬롯만 비워 보이게.
                                // senderMessageInfo 에 ?. 가 없어도 안전: senderMessageBody(= info?.message?…) 가
                                // non-null 인 이 블록에선 안전 호출 체인의 대우로 info 도 non-null 이 보장되고,
                                // K2 가 이를 smart cast 한다 (Kotlin 2.0+ "Local variables and further scopes").
                                // ?. 를 붙이면 "여기서 null 일 수 있다" 는 거짓 신호가 되어 생략.
                                SenderMessage(date = senderMessageInfo.createdAt.orEmpty(), body = it)
                            },
                        mindRecord = mindRecordsSummary,
                        timeLetterTotalCount = timeLettersCount,
                        afternoteTotalCount = afternotesResult?.totalCount,
                        afternoteIcons = afternotesResult?.items.orEmpty().toAfternoteIcons(),
                        // 자동 갱신이 화면을 교체해도 진행 중인 내려받기 다이얼로그·진행 상태는 잃지 않는다.
                        download = (current as? ReceiverHomeUiState.Success)?.download ?: ReceiverDownloadState.Idle,
                    )
                }
            }
        }

        private fun downloadAll() {
            updateDownload(ReceiverDownloadState.InProgress)
            viewModelScope.launch {
                receiverRepository
                    .downloadReceivedExport()
                    .onSuccess { bundle ->
                        receiverRepository
                            .saveReceivedExportToFile(bundle)
                            .onSuccess { updateDownload(ReceiverDownloadState.Done) }
                            .onFailure { e ->
                                errorReporter.recordReceiverFailure(ReceiverFailureStage.RECEIVED_EXPORT_SAVE, e)
                                updateDownload(
                                    ReceiverDownloadState.Failed(R.string.home_receiver_download_all_save_failed),
                                )
                            }
                    }.onFailure { e ->
                        if (e !is ReceiverFailure.ExportNotSupported) {
                            errorReporter.recordReceiverFailure(ReceiverFailureStage.RECEIVED_EXPORT_DOWNLOAD, e)
                        }
                        updateDownload(
                            ReceiverDownloadState.Failed(R.string.home_receiver_download_all_failed),
                        )
                    }
            }
        }

        private fun updateDownload(next: ReceiverDownloadState) {
            _uiState.update { state ->
                if (state is ReceiverHomeUiState.Success) state.copy(download = next) else state
            }
        }
    }

/** [ReceiverHomeViewModel] 이 홈 한 화면을 그리려고 병렬로 던지는 요청 수 — 전부 실패해야 Error 로 떨어진다. */
private const val HOME_REQUEST_COUNT = 4

private const val MAX_AFTERNOTE_ICONS = 4

private fun ReceiverMindRecords.toHomeSummary(): MindRecordSummary =
    MindRecordSummary(
        dailyQuestionCount = dailyQuestions.size,
        diaryCount = diaries.size,
    )

private fun List<AfterNoteListItem>.toAfternoteIcons(): List<AfternoteSourceIcon> =
    asSequence()
        // `type` 은 non-null 이라 `mapNotNull` 이 걸러 낼 것이 없다 (#1332 · #1452).
        // 남겨 두면 「종류를 모르는 항목이 섞여 들어온다」는 없는 사정을 읽게 한다.
        .map { it.type }
        .distinct()
        .take(MAX_AFTERNOTE_ICONS)
        .map { it.toSourceIcon() }
        .toList()

/**
 * 애프터노트 종류를 홈이 그릴 아이콘으로 옮긴다 (#926).
 *
 * res id 를 상태에 담지 않는다 — 담으면 ViewModel 이 Android 리소스에 묶이고, 형제
 * feature 의 `R` 을 가로질러 참조하게 된다. 어떤 그림인지는 core:ui 가 안다.
 */
private fun AfternoteType.toSourceIcon(): AfternoteSourceIcon =
    when (this) {
        AfternoteType.SOCIAL_NETWORK -> AfternoteSourceIcon.SocialNetwork
        AfternoteType.GALLERY_AND_FILES -> AfternoteSourceIcon.GalleryAndFiles
        AfternoteType.MEMORIAL -> AfternoteSourceIcon.Memorial
        AfternoteType.BUSINESS, AfternoteType.ESTATE -> AfternoteSourceIcon.Other
    }
