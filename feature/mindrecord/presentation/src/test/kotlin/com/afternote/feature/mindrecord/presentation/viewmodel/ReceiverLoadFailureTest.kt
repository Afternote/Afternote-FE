package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy

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
                    photoUploadRepository = PhotoUploadRepository { _, _ -> error("업로드는 호출되지 않는다") },
                    userRepository =
                        userRepository {
                            if (shouldFail) throw IOException("offline") else emptyList<Any>()
                        },
                    draftLoader = MindRecordDraftLoader(NoopDiaryRepository, NoopDailyQuestionRepository),
                )
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.receiverLoadError)

            shouldFail = false
            viewModel.loadReceivers()
            advanceUntilIdle()

            assertNull("재시도 성공 뒤에도 실패 안내가 남았다", viewModel.uiState.value.receiverLoadError)
        }

    private fun viewModel(failure: Throwable?): DiaryWriteViewModel =
        DiaryWriteViewModel(
            savedStateHandle = SavedStateHandle(emptyMap()),
            repository = NoopDiaryRepository,
            photoUploadRepository = PhotoUploadRepository { _, _ -> error("업로드는 호출되지 않는다") },
            userRepository = userRepository { failure?.let { throw it } ?: emptyList<Any>() },
            draftLoader = MindRecordDraftLoader(NoopDiaryRepository, NoopDailyQuestionRepository),
        )

    /** UserRepository 는 표면이 넓다 — 이 시나리오가 타는 호출만 답한다. */
    private fun userRepository(onGetReceivers: () -> Any): UserRepository =
        Proxy.newProxyInstance(
            UserRepository::class.java.classLoader,
            arrayOf(UserRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getReceivers" -> onGetReceivers()
                "getReceiverListFlow" -> flowOf(emptyList<Any>())
                else -> error("Unexpected call: ${method.name}")
            }
        } as UserRepository
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
