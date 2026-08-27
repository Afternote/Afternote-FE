package com.afternote.afternote_fe

import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.core.domain.model.UploadedFile
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import com.afternote.feature.timeletter.domain.usecase.ResolveTimeLetterBlocksUseCase
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteError
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class TimeLetterFlowAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUpTimeZone() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun registerWithoutReceiver_isBlockedAndShownToUser() {
        val repository = FakeTimeLetterRepository()
        val viewModel = viewModel(repository)
        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AfternoteTheme {
                TimeLetterWriteScreen(
                    uiState = uiState,
                    onRegisterClick = viewModel::register,
                    onErrorShown = viewModel::clearError,
                )
            }
        }

        composeRule.runOnIdle { viewModel.register("보낼 편지", mapOf(0L to "안녕")) }
        composeRule.onNodeWithText("수신자를 선택해주세요.").assertIsDisplayed()

        assertEquals(0, repository.createCalls.size)
    }

    @Test
    fun scheduledSave_failureThenRetry_keepsExactPayload() {
        val repository = FakeTimeLetterRepository()
        repository.createResults.addLast(Result.failure(IllegalStateException("offline")))
        repository.createResults.addLast(Result.success(Unit))
        val viewModel = viewModel(repository)
        composeRule.setContent { AfternoteTheme {} }
        composeRule.runOnIdle {
            viewModel.setRecipients(listOf(7L))
            viewModel.setSendAt("2026-09-03")
            viewModel.setSendTime(hour = 14, minute = 35)
            viewModel.addLinkBlock("https://example.test/memory")
            viewModel.register("가을 편지", mapOf(0L to "잊지 않을게"))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.error == TimeLetterWriteError.SaveFailed
        }
        assertFalse(viewModel.uiState.value.registered)

        composeRule.runOnIdle {
            viewModel.clearError()
            viewModel.register("가을 편지", mapOf(0L to "잊지 않을게"))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.registered }

        assertEquals(2, repository.createCalls.size)
        assertEquals(repository.createCalls.first(), repository.createCalls.last())
        val call = repository.createCalls.last()
        assertEquals("가을 편지", call.title)
        assertEquals("2026-09-03T14:35:00+09:00", call.sendAt)
        assertEquals(TimeLetterStatus.SCHEDULED, call.status)
        assertEquals(listOf(7L), call.receiverIds)
        assertEquals("잊지 않을게", call.blocks.first().textContent)
        assertEquals("https://example.test/memory", call.blocks.last().url)
    }

    @Test
    fun draftSave_usesDraftStatusWithoutSchedule() {
        val repository = FakeTimeLetterRepository()
        val viewModel = viewModel(repository)
        composeRule.setContent { AfternoteTheme {} }
        composeRule.runOnIdle {
            viewModel.setRecipients(listOf(7L))
            viewModel.saveDraft("임시 편지", mapOf(0L to "이어 쓸 내용"))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.savedAsDraft }

        val call = repository.createCalls.single()
        assertEquals(TimeLetterStatus.DRAFT, call.status)
        assertEquals(null, call.sendAt)
        assertEquals(listOf(7L), call.receiverIds)
    }

    private fun viewModel(repository: FakeTimeLetterRepository): TimeLetterWriteViewModel {
        val resolver =
            ResolveTimeLetterBlocksUseCase(
                FakePhotoUploadRepository(
                    onUpload = { uri, _ ->
                        val name = uri.substringAfterLast('/')
                        Result.success(
                            UploadedFile(
                                fileUrl = "https://cdn.test/$name",
                                fileKey = "timeletters/1/$name",
                            ),
                        )
                    },
                ),
            )
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolver),
            resolveTimeLetterBlocksUseCase = resolver,
            timeLetterRepository = repository,
            userRepository = appTestUserRepository(),
            fileMetadataRepository =
                object : FileMetadataRepository {
                    override suspend fun getFileName(uriString: String): String = "fixture"

                    override suspend fun getMimeType(uriString: String): String? = "application/pdf"
                },
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to null)),
        )
    }
}

private data class TimeLetterCreateCall(
    val title: String?,
    val blocks: List<NewTimeLetterBlock>,
    val sendAt: String?,
    val deliveryMode: TimeLetterDeliveryMode,
    val status: TimeLetterStatus,
    val receiverIds: List<Long>,
)

private class FakeTimeLetterRepository : TimeLetterRepository {
    val createCalls = mutableListOf<TimeLetterCreateCall>()
    val createResults = ArrayDeque<Result<Unit>>()

    override suspend fun getTimeLetters(): TimeLetterList = TimeLetterList(emptyList(), 0)

    override suspend fun getTemporaryTimeLetters(): TimeLetterList = TimeLetterList(emptyList(), 0)

    override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter = error("unexpected getTimeLetter")

    override suspend fun createTimeLetter(
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode,
        status: TimeLetterStatus,
        receiverIds: List<Long>,
    ): TimeLetter {
        createCalls += TimeLetterCreateCall(title, blocks, sendAt, deliveryMode, status, receiverIds)
        createResults.removeFirstOrNull()?.getOrThrow()
        return TimeLetter(1L, title, sendAt, null, status, emptyList(), receiverIds)
    }

    override suspend fun updateTimeLetter(
        timeLetterId: Long,
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode?,
        status: TimeLetterStatus?,
    ): TimeLetter = error("unexpected updateTimeLetter")

    override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) = error("unexpected deleteTimeLetters")

    override suspend fun deleteAllTemporary() = Unit
}
