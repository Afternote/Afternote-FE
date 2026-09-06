package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * 수신인 조회 실패가 «등록된 수신인 없음» 과 구분되는지 (#1019).
 *
 * 종전에는 `runCatching { }.onSuccess { }` 로 실패를 조용히 흘려, 화면이 두 경우를 같은 빈
 * 목록으로 그렸다. 수신자 최소 1명이 필수인 경로가 있어 조회 실패가 곧 «등록 불가» 로
 * 이어지는데, 그 사유가 화면 어디에도 없었다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverLoadFailureTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `조회에 실패하면 사유가 상태에 남는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(failure = IOException("offline"))
            advanceUntilIdle()

            assertNotNull("실패가 조용히 흡수됐다", viewModel.uiState.value.receiverLoadError)
            assertEquals(emptyList<Any>(), viewModel.uiState.value.receivers)
        }

    @Test
    fun `실패 문구는 예외 원문이 아니라 도메인 문구다`() =
        runTest(dispatcher) {
            // 오프라인이면 예외 message 가 «Unable to resolve host "afternote.kro.kr" …» 이다.
            // 그대로 쓰면 사용자에게 기술 원문이 노출된다 (#614 가 수신자 화면에서 막은 것과 같다).
            val viewModel = viewModel(failure = IOException("Unable to resolve host \"afternote.kro.kr\""))
            advanceUntilIdle()

            assertEquals(
                UiText.Resource(R.string.mindrecord_error_receiver_load_failed),
                viewModel.uiState.value.receiverLoadError,
            )
        }

    @Test
    fun `조회에 성공하면 사유가 남지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(failure = null)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.receiverLoadError)
        }

    @Test
    fun `재시도가 성공하면 안내가 걷힌다`() =
        runTest(dispatcher) {
            // 실패 → 재시도 성공. 안내가 남아 있으면 «불러왔는데 실패 문구» 가 된다.
            var shouldFail = true
            val viewModel =
                DiaryWriteViewModel(
                    savedStateHandle = SavedStateHandle(emptyMap()),
                    repository = NoopDiaryRepository,
                    photoUploadRepository = FakePhotoUploadRepository.strict(),
                    userRepository =
                        userRepository {
                            if (shouldFail) throw IOException("offline") else emptyList()
                        },
                    draftLoader = LoadMindRecordDraftsUseCase(NoopDiaryRepository, NoopDailyQuestionRepository),
                    errorReporter = RecordingErrorReporter(),
                )
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.receiverLoadError)

            shouldFail = false
            viewModel.loadReceivers()
            advanceUntilIdle()

            assertNull("재시도 성공 뒤에도 실패 안내가 남았다", viewModel.uiState.value.receiverLoadError)
        }

    @Test
    fun `재시도 중에는 미등록 문구가 아니라 조회 중이다`() =
        runTest(dispatcher) {
            // 시트는 «오류 문구 ?: 미등록 문구» 로 고른다. 재시도를 누른 순간 오류만 지워지고
            // 목록은 아직 비어 있으므로, 조회 중 상태가 없으면 그 창에서 «등록된 수신자가
            // 없습니다» 가 뜬다 — 이 PR 이 없애려던 혼동이 사용자 손으로 되돌아온다.
            //
            // 창의 길이는 서버가 정한다. 응답이 없으면 연결 10초·호출 30초까지 간다 (#1019 리뷰).
            // fake 의 람다가 «요청이 떠 있는 그 창» 에서 실행되므로, 거기서 상태를 읽으면
            // 사용자가 그 순간 보는 화면을 그대로 관찰할 수 있다.
            lateinit var viewModel: DiaryWriteViewModel
            var shouldFail = true
            var stateInFlight: DiaryWriteUiState? = null
            viewModel =
                DiaryWriteViewModel(
                    savedStateHandle = SavedStateHandle(emptyMap()),
                    repository = NoopDiaryRepository,
                    photoUploadRepository = FakePhotoUploadRepository.strict(),
                    userRepository =
                        userRepository {
                            if (shouldFail) {
                                throw IOException("offline")
                            } else {
                                stateInFlight = viewModel.uiState.value
                                emptyList()
                            }
                        },
                    draftLoader = LoadMindRecordDraftsUseCase(NoopDiaryRepository, NoopDailyQuestionRepository),
                    errorReporter = RecordingErrorReporter(),
                )
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.receiverLoadError)

            shouldFail = false
            viewModel.loadReceivers()
            advanceUntilIdle()

            val inFlight = checkNotNull(stateInFlight) { "재시도가 조회를 걸지 않았다" }
            // 오류는 이미 걷혔고 목록은 아직 비었다 — 이 창을 «미등록» 으로 읽으면 안 된다.
            assertNull(inFlight.receiverLoadError)
            assertEquals(emptyList<Any>(), inFlight.receivers)
            assertTrue("조회 중 상태가 없어 시트가 «미등록» 을 고른다", inFlight.isReceiverLoading)

            assertFalse("응답 뒤에도 조회 중이 남았다", viewModel.uiState.value.isReceiverLoading)
        }

    private fun viewModel(failure: Throwable?): DiaryWriteViewModel =
        DiaryWriteViewModel(
            savedStateHandle = SavedStateHandle(emptyMap()),
            repository = NoopDiaryRepository,
            photoUploadRepository = FakePhotoUploadRepository.strict(),
            userRepository = userRepository { failure?.let { throw it } ?: emptyList() },
            draftLoader = LoadMindRecordDraftsUseCase(NoopDiaryRepository, NoopDailyQuestionRepository),
            errorReporter = RecordingErrorReporter(),
        )

    /** UserRepository 는 표면이 넓다 — 이 시나리오가 타는 호출만 답한다. */
    private fun userRepository(onGetReceivers: suspend () -> List<Receiver>): FakeUserRepository =
        FakeUserRepository.strict().apply {
            this.onGetReceivers = onGetReceivers
            onReceiverListFlow = { flowOf(emptyList()) }
        }
}

private object NoopDiaryRepository : DiaryRepository {
    override suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean?,
    ): Result<DiaryList> = Result.success(DiaryList(diaries = emptyList(), monthDiaryCount = 0, weeklyDominantMood = null))

    override suspend fun create(payload: DiaryCreatePayload): Result<Unit> = error("호출되면 안 됨")

    override suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit> = error("호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}

private object NoopDailyQuestionRepository : DailyQuestionRepository {
    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ) = Result.success(emptyList<com.afternote.feature.mindrecord.domain.model.DailyQuestion>())

    override suspend fun getToday() = error("호출되면 안 됨")

    override suspend fun create(payload: com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload) = error("호출되면 안 됨")

    override suspend fun update(
        id: Long,
        payload: com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload,
    ) = error("호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}
