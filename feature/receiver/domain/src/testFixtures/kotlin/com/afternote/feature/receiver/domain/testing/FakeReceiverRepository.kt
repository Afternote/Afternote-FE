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
 * [masterKeyState] 에 값을 넣으면 [masterKeyFlow] 구독자와 [currentMasterKey] 가 같은 값을 본다.
 * 저장소 기본 동작으로 표현하기 어려운 실패·경합·응답 순서는 `onX` 로 갈아끼운다.
 */
class FakeReceiverRepository(
    initialMasterKey: String? = null,
    var afterNotes: AfterNotesListResult = EMPTY_AFTER_NOTES,
    details: Map<Long, ReceivedAfternoteDetail> = emptyMap(),
    var exportBundle: ReceivedExportBundle = ReceivedExportBundle(),
    var senderMessage: SenderMessageInfo? = null,
    var pagedAfterNotes: Flow<PagingData<AfterNoteListItem>> = flowOf(PagingData.empty()),
    var onMasterKeyFlow: (() -> Flow<String?>)? = null,
    var onCurrentMasterKey: (suspend () -> String?)? = null,
    var onSaveMasterKey: (suspend (String) -> Unit)? = null,
    var onGetPagedReceivedAfternotes: (() -> Flow<PagingData<AfterNoteListItem>>)? = null,
    var onGetReceivedAfterNotes: (suspend () -> Result<AfterNotesListResult>)? = null,
    var onGetReceivedAfternoteDetail: (suspend (Long) -> Result<ReceivedAfternoteDetail>)? = null,
    var onDownloadReceivedExport: (suspend () -> Result<ReceivedExportBundle>)? = null,
    var onSaveReceivedExportToFile: (suspend (ReceivedExportBundle) -> Result<Unit>)? = null,
    var onLoadSenderMessage: (suspend () -> Result<SenderMessageInfo?>)? = null,
) : ReceiverRepository {
    /** 테스트가 직접 값을 밀어 넣는 수신자 인증 코드 상태. */
    val masterKeyState = MutableStateFlow(initialMasterKey?.trim()?.takeIf(String::isNotEmpty))
    val details = ConcurrentHashMap(details)

    val savedMasterKeys = CopyOnWriteArrayList<String>()
    val requestedDetailIds = CopyOnWriteArrayList<Long>()
    val savedBundles = CopyOnWriteArrayList<ReceivedExportBundle>()

    private val masterKeyFlowCounter = AtomicInteger()
    private val currentMasterKeyCounter = AtomicInteger()
    private val pagedAfterNotesCounter = AtomicInteger()
    private val afterNotesCounter = AtomicInteger()
    private val downloadCounter = AtomicInteger()
    private val senderMessageCounter = AtomicInteger()

    val masterKeyFlowCalls: Int
        get() = masterKeyFlowCounter.get()

    val currentMasterKeyCalls: Int
        get() = currentMasterKeyCounter.get()

    val getPagedReceivedAfternotesCalls: Int
        get() = pagedAfterNotesCounter.get()

    val getReceivedAfterNotesCalls: Int
        get() = afterNotesCounter.get()

    val downloadCalls: Int
        get() = downloadCounter.get()

    val loadSenderMessageCalls: Int
        get() = senderMessageCounter.get()

    override val masterKeyFlow: Flow<String?>
        get() {
            masterKeyFlowCounter.incrementAndGet()
            return onMasterKeyFlow?.invoke() ?: masterKeyState
        }

    override suspend fun currentMasterKey(): String? {
        currentMasterKeyCounter.incrementAndGet()
        onCurrentMasterKey?.let { return it() }
        return masterKeyState.value
    }

    override suspend fun saveMasterKey(code: String) {
        savedMasterKeys += code
        onSaveMasterKey?.let {
            it(code)
            return
        }
        masterKeyState.value = code.trim().takeIf(String::isNotEmpty)
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

        /** 모든 계약 경로를 닫되 [masterKeyState] 자체는 테스트가 준비 상태로 쓸 수 있다. */
        fun strict(initialMasterKey: String? = null): FakeReceiverRepository =
            FakeReceiverRepository(
                initialMasterKey = initialMasterKey,
                onMasterKeyFlow = { unexpectedCall("ReceiverRepository.authCodeFlow") },
                onCurrentMasterKey = { unexpectedCall("ReceiverRepository.currentAuthCode") },
                onSaveMasterKey = { unexpectedCall("ReceiverRepository.saveAuthCode") },
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
