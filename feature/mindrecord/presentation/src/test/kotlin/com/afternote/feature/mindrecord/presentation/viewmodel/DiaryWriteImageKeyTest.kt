package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.model.UploadedFile
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * 일기 본문 이미지가 서버 계약대로 fileKey 로 나가는지 (#1016).
 *
 * 서버는 본문 `img src` 에서 fileKey 를 받아 staging→permanent 로 옮기고 전체 URL 로 다시
 * 쓴다. 전체 URL 을 그대로 보내면 그 앞에 호스트를 한 번 더 붙여 403 이 된다. 데일리질문에는
 * 이 변환이 있었는데(#549) 일기 경로에만 없어서, 이미지 업로드가 고쳐지는 순간(#953)
 * 일기 본문이 깨진 이미지가 될 자리였다.
 */
class DiaryWriteImageKeyTest {
    // submit() 이 viewModelScope(= Main)에서 돈다 — 테스트 디스패처를 깔아야 실제로 실행된다.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `등록 시 방금 업로드한 이미지의 src 는 fileKey 로 나간다`() {
        var sent: String? = null
        val viewModel = viewModel(onCreate = { sent = it.content })

        val previewUrl = runBlocking { viewModel.uploadMedia("content://picked") }
        viewModel.onTitleChanged("제목")
        viewModel.onContentChanged("<p>본문</p><img src=\"$previewUrl\" />")
        viewModel.onMoodSelected(TodayMood.HAPPY)
        viewModel.submit()

        assertEquals("<p>본문</p><img src=\"mindrecords/staging/13/a.png\" />", sent)
    }

    @Test
    fun `이미 저장된 영구 URL 은 건드리지 않는다`() {
        // 서버가 이미 permanent 로 옮긴 이미지다. 키로 바꾸면 다시 옮기려다 실패한다.
        var sent: String? = null
        val viewModel = viewModel(onCreate = { sent = it.content })
        val storedHtml = "<p>이어쓰기</p><img src=\"https://cdn.example.net/mindrecords/permanent/13/old.png\" />"

        viewModel.onTitleChanged("제목")
        viewModel.onContentChanged(storedHtml)
        viewModel.onMoodSelected(TodayMood.SOSO)
        viewModel.submit()

        assertEquals(storedHtml, sent)
    }

    private fun viewModel(onCreate: (DiaryCreatePayload) -> Unit): DiaryWriteViewModel =
        DiaryWriteViewModel(
            savedStateHandle = SavedStateHandle(emptyMap()),
            repository = RecordingDiaryRepository(onCreate),
            photoUploadRepository =
                PhotoUploadRepository { _, _ ->
                    Result.success(UploadedFile(fileUrl = UPLOADED_URL, fileKey = UPLOADED_KEY))
                },
            userRepository = noReceiverUserRepository(),
            draftLoader = MindRecordDraftLoader(RecordingDiaryRepository {}, NoDailyQuestionRepository),
        )

    private companion object {
        const val UPLOADED_URL = "https://cdn.example.net/mindrecords/staging/13/a.png"
        const val UPLOADED_KEY = "mindrecords/staging/13/a.png"
    }
}

private class RecordingDiaryRepository(
    private val onCreate: (DiaryCreatePayload) -> Unit,
) : DiaryRepository {
    override suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean?,
    ): Result<DiaryList> = Result.success(DiaryList(diaries = emptyList(), monthDiaryCount = 0, weeklyDominantMood = null))

    override suspend fun create(payload: DiaryCreatePayload): Result<Unit> {
        onCreate(payload)
        return Result.success(Unit)
    }

    override suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit> = error("이 시나리오에서 호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("이 시나리오에서 호출되면 안 됨")
}

private object NoDailyQuestionRepository : DailyQuestionRepository {
    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ) = Result.success(emptyList<com.afternote.feature.mindrecord.domain.model.DailyQuestion>())

    override suspend fun getToday() = error("이 시나리오에서 호출되면 안 됨")

    override suspend fun create(payload: com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload) =
        error("이 시나리오에서 호출되면 안 됨")

    override suspend fun update(
        id: Long,
        payload: com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload,
    ) = error("이 시나리오에서 호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("이 시나리오에서 호출되면 안 됨")
}

/** UserRepository 는 표면이 넓다 — 이 시나리오가 실제로 타는 두 호출만 답한다. */
private fun noReceiverUserRepository(): UserRepository =
    Proxy.newProxyInstance(
        UserRepository::class.java.classLoader,
        arrayOf(UserRepository::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getReceiverListFlow" -> flowOf(emptyList<Any>())
            "getReceivers" -> emptyList<Any>()
            else -> error("Unexpected user repository call: ${method.name}")
        }
    } as UserRepository
