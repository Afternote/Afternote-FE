package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
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
    fun `업로드는 미리보기 URL 을 돌려주고 진행 플래그를 내린다`() {
        // 종전에는 «첫 업로드» 를 화면 상태의 imageUrl 로도 집었다. 조건이 순서뿐이라 첨부가
        // 미디어 전체로 넓어진 뒤에는 음성을 먼저 붙이면 그 자리에 `.m4a` 가 실렸다 (#1195).
        val viewModel = viewModel()

        val previewUrl = runBlocking { viewModel.uploadMedia("content://picked") }

        assertEquals("https://cdn.example.net/mindrecords/staging/13/a.png", previewUrl)
        assertEquals(false, viewModel.uiState.value.isUploadingImage)
    }

    @Test
    fun `화면 상태에 대표 이미지 필드가 없다`() {
        // **구조로만 지킬 수 있는 자리다.** 그 필드는 읽는 곳이 0 이었다 — 되살려도 화면도 payload 도
        // 달라지지 않으니 동작 테스트로는 잡히지 않는다(실제로 되살려 보고 확인했다). 그래서
        // 「필드가 없다」 자체를 계약으로 고정한다.
        //
        // 다시 필요해지면 이 단언을 지우는 것이 곧 «대표 이미지 개념을 다시 도입한다» 는 선언이 된다.
        // 그때는 종류 판정(MIME·확장자) 없이 «첫 번째» 로 집지 않아야 한다 (#1195).
        // data class 의 toString 이 프로퍼티 목록을 그대로 드러낸다 — reflect 의존 없이 형태를 본다.
        val shape = DiaryWriteUiState().toString()

        assertEquals(false, shape.contains("imageUrl="))
    }

    @Test
    fun `제출 payload 는 서버 계약 필드만 담는다`() {
        // 서버 `DiaryCreateRequest` 에 `imageUrl` 이 없다 — 그게 이 PR 의 근거다. 대표 이미지가
        // payload 로 새어 나가면 여기서 갈린다 (#1195).
        //
        // (필드 목록을 여기 나열하지 않는다. 초안에 `date` 를 빠뜨렸는데 서버가 그 사이 추가한
        //  것이었다 — 목록은 이 테스트가 지키는 것도 아니면서 낡을 뿐이다.)
        var sent: DiaryCreatePayload? = null
        val viewModel = viewModel(onCreate = { sent = it })

        runBlocking { viewModel.uploadMedia("content://picked") }
        viewModel.onTitleChanged("제목")
        viewModel.onContentChanged("<p>본문</p>")
        viewModel.onMoodSelected(TodayMood.HAPPY)
        viewModel.submit()

        val payload = requireNotNull(sent)
        assertEquals("제목", payload.title)
        assertEquals("<p>본문</p>", payload.content)
        assertEquals(TodayMood.HAPPY, payload.todayMood)
        assertEquals(emptyList<Long>(), payload.receiverIds)
        // 값 확인만으로는 **초과 필드**를 못 본다 — 누군가 payload 에 `imageUrl` 을 더하면 위 네
        // 단언은 그대로 통과한다. 형태로 막는다 (#1195 리뷰). 옆 테스트와 같은 방식이다.
        assertEquals(false, payload.toString().contains("imageUrl="))
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

    @Test
    fun `프로세스 사망으로 되살아나도 방금 올린 이미지가 fileKey 로 나간다`() {
        // 에디터 본문은 rememberSaveable(RichTextState.Saver) 라 죽었다 살아나도 HTML 이
        // 그대로 복원되고, 그 값이 다시 ViewModel 로 흘러든다. 대응표만 인메모리면 복원된
        // 본문에는 전체 URL 이 남았는데 바꿀 근거가 사라진 상태가 되어, 그대로 서버로 나가
        // 호스트가 한 번 더 붙고 403 이 된다 (#549 재발, #1125 리뷰).
        val handle = SavedStateHandle(emptyMap())
        val previewUrl = runBlocking { viewModel(savedStateHandle = handle).uploadMedia("content://picked") }

        // 같은 SavedStateHandle 로 새 ViewModel — 프로세스 사망 뒤 복원과 같은 자리다.
        var sent: String? = null
        val revived = viewModel(onCreate = { sent = it.content }, savedStateHandle = handle)
        revived.onTitleChanged("제목")
        revived.onContentChanged("<p>본문</p><img src=\"$previewUrl\" />")
        revived.onMoodSelected(TodayMood.HAPPY)
        revived.submit()

        assertEquals("<p>본문</p><img src=\"$UPLOADED_KEY\" />", sent)
    }

    private fun viewModel(
        onCreate: (DiaryCreatePayload) -> Unit = {},
        savedStateHandle: SavedStateHandle = SavedStateHandle(emptyMap()),
    ): DiaryWriteViewModel =
        DiaryWriteViewModel(
            savedStateHandle = savedStateHandle,
            repository = RecordingDiaryRepository(onCreate),
            photoUploadRepository =
                FakePhotoUploadRepository(
                    uploadedUrl = UPLOADED_URL,
                    uploadedKey = UPLOADED_KEY,
                ),
            userRepository = noReceiverUserRepository(),
            draftLoader = LoadMindRecordDraftsUseCase(RecordingDiaryRepository {}, NoDailyQuestionRepository),
            errorReporter = RecordingErrorReporter(),
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
private fun noReceiverUserRepository(): FakeUserRepository =
    FakeUserRepository.strict().apply {
        onReceiverListFlow = { flowOf(emptyList()) }
        onGetReceivers = { emptyList() }
    }
