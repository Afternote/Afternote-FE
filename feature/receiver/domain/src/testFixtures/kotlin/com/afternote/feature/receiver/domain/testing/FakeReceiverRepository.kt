package com.afternote.feature.receiver.domain.testing

import androidx.paging.PagingData
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedExportBundle
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [ReceiverRepository] fake 정본 (#1030, #1042).
 *
 * [authCodeState] 에 값을 넣으면 [authCodeFlow] 구독자와 [currentAuthCode] 가 같은 값을 본다.
 * 저장소 기본 동작으로 표현하기 어려운 실패·경합·응답 순서는 `onX` 로 갈아끼운다.
 */
class FakeReceiverRepository(
    initialAuthCode: String? = null,
    var afterNotes: AfterNotesListResult = EMPTY_AFTER_NOTES,
    details: Map<Long, ReceivedAfternoteDetail> = emptyMap(),
    var exportBundle: ReceivedExportBundle = ReceivedExportBundle(),
    var senderMessage: SenderMessageInfo? = null,
    var pagedAfterNotes: Flow<PagingData<AfterNoteListItem>> = flowOf(PagingData.empty()),
    var onAuthCodeFlow: (() -> Flow<String?>)? = null,
    var onCurrentAuthCode: (suspend () -> String?)? = null,
    var onSaveAuthCode: (suspend (String) -> Unit)? = null,
    var onGetPagedReceivedAfternotes: (() -> Flow<PagingData<AfterNoteListItem>>)? = null,
    var onGetReceivedAfterNotes: (suspend () -> Result<AfterNotesListResult>)? = null,
    var onGetReceivedAfternoteDetail: (suspend (Long) -> Result<ReceivedAfternoteDetail>)? = null,
    var onDownloadReceivedExport: (suspend () -> Result<ReceivedExportBundle>)? = null,
    var onSaveReceivedExportToFile: (suspend (ReceivedExportBundle) -> Result<Unit>)? = null,
    var onLoadSenderMessage: (suspend () -> Result<SenderMessageInfo?>)? = null,
) : ReceiverRepository {
    /** 테스트가 직접 값을 밀어 넣는 수신자 인증 코드 상태. */
    val authCodeState = MutableStateFlow(initialAuthCode?.trim()?.takeIf(String::isNotEmpty))
    val details = ConcurrentHashMap(details)

    val savedAuthCodes = CopyOnWriteArrayList<String>()
    val requestedDetailIds = CopyOnWriteArrayList<Long>()
    val savedBundles = CopyOnWriteArrayList<ReceivedExportBundle>()

    private val authCodeFlowCounter = AtomicInteger()
    private val currentAuthCodeCounter = AtomicInteger()
    private val pagedAfterNotesCounter = AtomicInteger()
    private val afterNotesCounter = AtomicInteger()
    private val downloadCounter = AtomicInteger()
    private val senderMessageCounter = AtomicInteger()

    val authCodeFlowCalls: Int
        get() = authCodeFlowCounter.get()

    val currentAuthCodeCalls: Int
        get() = currentAuthCodeCounter.get()

    val getPagedReceivedAfternotesCalls: Int
        get() = pagedAfterNotesCounter.get()

    val getReceivedAfterNotesCalls: Int
        get() = afterNotesCounter.get()

    val downloadCalls: Int
        get() = downloadCounter.get()

    val loadSenderMessageCalls: Int
        get() = senderMessageCounter.get()

    override val authCodeFlow: Flow<String?>
        get() {
            authCodeFlowCounter.incrementAndGet()
            return onAuthCodeFlow?.invoke() ?: authCodeState
        }

    override suspend fun currentAuthCode(): String? {
        currentAuthCodeCounter.incrementAndGet()
        onCurrentAuthCode?.let { return it() }
        return authCodeState.value
    }

    override suspend fun saveAuthCode(code: String) {
        savedAuthCodes += code
        onSaveAuthCode?.let {
            it(code)
            return
        }
        authCodeState.value = code.trim().takeIf(String::isNotEmpty)
    }

    override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> {
        pagedAfterNotesCounter.incrementAndGet()
        return onGetPagedReceivedAfternotes?.invoke() ?: pagedAfterNotes
    }

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> {
        afterNotesCounter.incrementAndGet()
        onGetReceivedAfterNotes?.let { return it() }
        return Result.success(afterNotes)
    }

    override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> {
        requestedDetailIds += afternoteId
        onGetReceivedAfternoteDetail?.let { return it(afternoteId) }
        return runCatching {
            requireNotNull(details[afternoteId]) {
                "수신 애프터노트 상세가 없다: afternoteId=$afternoteId"
            }
        }
    }

    override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> {
        downloadCounter.incrementAndGet()
        onDownloadReceivedExport?.let { return it() }
        return Result.success(exportBundle)
    }

    override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> {
        savedBundles += bundle
        onSaveReceivedExportToFile?.let { return it(bundle) }
        return Result.success(Unit)
    }

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> {
        senderMessageCounter.incrementAndGet()
        onLoadSenderMessage?.let { return it() }
        return Result.success(senderMessage)
    }

    companion object {
        private val EMPTY_AFTER_NOTES = AfterNotesListResult(items = emptyList(), totalCount = 0)

        /** 모든 계약 경로를 닫되 [authCodeState] 자체는 테스트가 준비 상태로 쓸 수 있다. */
        fun strict(initialAuthCode: String? = null): FakeReceiverRepository =
            FakeReceiverRepository(
                initialAuthCode = initialAuthCode,
                onAuthCodeFlow = { unexpectedCall("ReceiverRepository.authCodeFlow") },
                onCurrentAuthCode = { unexpectedCall("ReceiverRepository.currentAuthCode") },
                onSaveAuthCode = { unexpectedCall("ReceiverRepository.saveAuthCode") },
                onGetPagedReceivedAfternotes = {
                    unexpectedCall("ReceiverRepository.getPagedReceivedAfternotes")
                },
                onGetReceivedAfterNotes = {
                    unexpectedCall("ReceiverRepository.getReceivedAfterNotes")
                },
                onGetReceivedAfternoteDetail = {
                    unexpectedCall("ReceiverRepository.getReceivedAfternoteDetail")
                },
                onDownloadReceivedExport = {
                    unexpectedCall("ReceiverRepository.downloadReceivedExport")
                },
                onSaveReceivedExportToFile = {
                    unexpectedCall("ReceiverRepository.saveReceivedExportToFile")
                },
                onLoadSenderMessage = {
                    unexpectedCall("ReceiverRepository.loadSenderMessage")
                },
            )
    }
}
