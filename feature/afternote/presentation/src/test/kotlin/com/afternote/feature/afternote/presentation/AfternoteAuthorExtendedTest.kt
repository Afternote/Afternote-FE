package com.afternote.feature.afternote.presentation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.ProcessingMethod
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.detail.AfternoteDetailDeleteResult
import com.afternote.feature.afternote.presentation.detail.AfternoteDetailUiState
import com.afternote.feature.afternote.presentation.detail.AfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.detail.DetailContentUiModel
import com.afternote.feature.afternote.presentation.detail.account.AccountDetailScreen
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorBody
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorScreen
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.editor.SaveAfternotePayloadBuilder
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.rememberAfternoteEditorState
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import com.afternote.feature.afternote.presentation.R as AfternoteFeatureR

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class AfternoteAuthorExtendedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailEdit_updatesExactPrefilledPayloadThroughEditorRoute() {
        val repository =
            FakeAfternoteRepository.strict().apply {
                onGetDetail = { Result.success(authorDetail()) }
                onUpdate = { id, _ -> Result.success(id) }
            }
        val detailViewModel = detailViewModel(repository, itemId = 73L)
        var routedItemId by mutableStateOf<String?>(null)
        var editorViewModel: AfternoteEditorViewModel? = null
        var editorState: AfternoteEditorState? = null

        composeRule.setContent {
            AfternoteTheme {
                val itemId = routedItemId
                if (itemId == null) {
                    AuthorDetailForEdit(
                        viewModel = detailViewModel,
                        onEdit = { id ->
                            editorViewModel = editorViewModel(repository, id.toLong())
                            routedItemId = id
                        },
                    )
                } else {
                    AuthorEditorForUpdate(
                        itemId = itemId.toLong(),
                        viewModel = checkNotNull(editorViewModel),
                        onStateReady = { editorState = it },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("수정").performClick()
        composeRule.onNodeWithText("수정하기").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            routedItemId == "73" && editorState != null
        }
        composeRule.onNodeWithText("old@example.test").assertIsDisplayed()
        composeRule
            .onNode(hasSetTextAction() and hasText("old@example.test"))
            .performTextReplacement("edited@example.test")
        composeRule.runOnIdle {
            checkNotNull(editorViewModel).editProcessingMethod(1, "계정 보존")
        }
        composeRule.onNodeWithText("계정 보존").performScrollTo().assertIsDisplayed()
        val topBarRegister =
            hasText("등록", substring = false) and
                hasClickAction() and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        composeRule.onNode(topBarRegister).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { repository.updateCalls.size == 1 }
        val (updatedId, payload) = repository.updateCalls.single()
        assertEquals(73L, updatedId)
        assertEquals(AfternoteType.SOCIAL_NETWORK, payload.type)
        assertEquals("Instagram", payload.title)
        assertEquals(listOf("계정 보존"), payload.processingMethods)
        assertEquals(
            listOf(LeaveMessageBlock(title = "마지막 말", body = "기억해 줘")),
            payload.leaveMessageBlocks,
        )
        assertEquals("edited@example.test", payload.credentials?.id)
        assertEquals("old-password", payload.credentials?.password)
        assertEquals(listOf(ReceiverRefPayload(7L)), payload.receivers)
        assertNull(payload.memorial)
        assertEquals(listOf(73L, 73L), repository.requestedDetailIds)
        assertEquals("73", routedItemId)
    }

    @Test
    fun serviceSelectionSheet_dismissPreservesCustomPrefillAndSearchSelectsExactCatalogValue() {
        val repository =
            FakeAfternoteRepository.strict().apply {
                onGetDetail = { Result.success(authorDetail()) }
            }
        val viewModel = editorViewModel(repository, itemId = 73L)
        var editorState: AfternoteEditorState? = null

        composeRule.setContent {
            AfternoteTheme {
                AuthorEditorForUpdate(
                    itemId = 73L,
                    viewModel = viewModel,
                    onStateReady = { editorState = it },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isPrefillLoading && editorState != null
        }
        composeRule.onNodeWithText("Instagram").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("소셜 네트워크 서비스 선택").assertIsDisplayed()
        composeRule.onNodeWithText("서비스 검색하기").performTextInput("  페이  ")
        composeRule.onNodeWithText("페이스북").assertIsDisplayed()
        composeRule.onNodeWithText("인스타그램").assertDoesNotExist()

        composeRule.runOnIdle { checkNotNull(editorState).dismissServiceSelectionSheet() }
        composeRule.onNodeWithText("소셜 네트워크 서비스 선택").assertDoesNotExist()
        assertEquals("Instagram", viewModel.currentForm().selectedService)
        assertEquals("", checkNotNull(editorState).serviceSearchQueryState.text.toString())

        composeRule.onNodeWithText("Instagram").performClick()
        composeRule.onNodeWithText("인스타그램").assertIsDisplayed()
        composeRule.onNodeWithText("서비스 검색하기").performTextInput("페이")
        composeRule.onNodeWithText("페이스북").performClick()

        composeRule.onNodeWithText("소셜 네트워크 서비스 선택").assertDoesNotExist()
        composeRule.onNodeWithText("페이스북").assertIsDisplayed()
        assertEquals("페이스북", viewModel.currentForm().selectedService)
        assertEquals("", checkNotNull(editorState).serviceSearchQueryState.text.toString())
    }

    @Test
    fun repeatedValidation_afterPopupConfirm_showsAgainWithoutSaveCall() {
        val repository =
            FakeAfternoteRepository.strict().apply {
                onGetDetail = {
                    // 수신자는 선택 항목(#951)이라 비어 있어도 오류에 끼지 않는다 — 계정정보 누락만 단일 오류로 떠야 한다.
                    Result.success(
                        authorDetail().copy(
                            receivers = emptyList(),
                            content =
                                DetailContent.SocialNetwork(
                                    credentials = DetailCredentials(id = "", password = ""),
                                    processingMethods = listOf("계정 삭제"),
                                ),
                        ),
                    )
                }
            }
        val viewModel = editorViewModel(repository, itemId = 73L)
        var editorState: AfternoteEditorState? = null
        val validationMessage =
            RuntimeEnvironment
                .getApplication()
                .getString(R.string.afternote_validation_account_credentials_required)

        composeRule.setContent {
            AfternoteTheme {
                AuthorEditorForUpdate(
                    itemId = 73L,
                    viewModel = viewModel,
                    onStateReady = { editorState = it },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isPrefillLoading &&
                editorState != null
        }
        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
        val topBarRegister =
            hasText("등록", substring = false) and
                hasClickAction() and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

        composeRule.onNode(topBarRegister).performClick()
        composeRule.onNodeWithText(validationMessage).assertIsDisplayed()
        val firstOccurrence = requireNotNull(viewModel.uiState.value.errorEvent).occurrence
        assertEquals(0, repository.updateCalls.size)

        composeRule.onNodeWithText("확인").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.errorEvent == null
        }
        composeRule.onNodeWithText(validationMessage).assertDoesNotExist()

        composeRule.onNode(topBarRegister).performClick()
        composeRule.onNodeWithText(validationMessage).assertIsDisplayed()
        val secondOccurrence = requireNotNull(viewModel.uiState.value.errorEvent).occurrence

        assertNotEquals(firstOccurrence, secondOccurrence)
        assertEquals(0, repository.updateCalls.size)
    }

    @Test
    fun delete_cancelThenFailureAndRetrySuccess_callsOnlyAfterConfirmation() {
        val deleteResults =
            ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("offline")),
                    Result.success(Unit),
                ),
            )
        val repository =
            FakeAfternoteRepository.strict().apply {
                onGetDetail = { Result.success(authorDetail()) }
                onDelete = { deleteResults.removeFirst() }
            }
        val viewModel = detailViewModel(repository, itemId = 73L)
        val deletedIds = mutableListOf<Long>()

        composeRule.setContent {
            AfternoteTheme {
                AuthorDetailForDelete(
                    viewModel = viewModel,
                    deletedIds = deletedIds,
                )
            }
        }

        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
        openDeleteDialog()
        assertEquals(emptyList<Long>(), repository.deletedIds)
        composeRule.onNodeWithText("취소하기").performClick()
        assertEquals(emptyList<Long>(), repository.deletedIds)

        openDeleteDialog()
        composeRule.onNodeWithText("삭제하기").performClick()
        composeRule.onNodeWithText("삭제에 실패했습니다.").assertIsDisplayed()
        assertEquals(listOf(73L), repository.deletedIds)
        assertEquals(emptyList<Long>(), deletedIds)

        openDeleteDialog()
        composeRule.onNodeWithText("삭제하기").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { deletedIds == listOf(73L) }

        assertEquals(listOf(73L, 73L), repository.deletedIds)
        assertEquals(listOf(73L), deletedIds)
    }

    private fun openDeleteDialog() {
        composeRule.onNodeWithContentDescription("수정").performClick()
        composeRule.onNodeWithText("삭제하기").performClick()
        composeRule.onNodeWithText("취소하기").assertIsDisplayed()
    }
}

@Composable
private fun AuthorDetailForEdit(
    viewModel: AfternoteDetailViewModel,
    onEdit: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        AfternoteDetailUiState.Loading -> {
            CircularProgressIndicator()
        }

        is AfternoteDetailUiState.Error -> {
            Text("detail error")
        }

        is AfternoteDetailUiState.Success -> {
            val account = state.contentUiModel as DetailContentUiModel.SocialNetwork
            AccountDetailScreen(
                onBackClick = {},
                content = account.content,
                onEditClick = { onEdit(state.detailId.toString()) },
                onDeleteConfirm = {},
            )
        }
    }
}

@Composable
private fun AuthorEditorForUpdate(
    itemId: Long,
    viewModel: AfternoteEditorViewModel,
    onStateReady: (AfternoteEditorState) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state =
        rememberAfternoteEditorState(
            getCurrentForm = viewModel::currentForm,
            setType = viewModel::setType,
            setService = viewModel::setService,
            setMemorialPhoto = viewModel::setMemorialPhoto,
            removeMemorialPhoto = viewModel::removeMemorialPhoto,
            setMemorialVideo = viewModel::setMemorialVideo,
            removeMemorialVideo = viewModel::removeMemorialVideo,
            addReceiverIfAbsent = viewModel::addReceiverIfAbsent,
            applyPrefill = viewModel::applyPrefill,
            setMemorialThumbnail = viewModel::setMemorialThumbnail,
            deleteReceiver = viewModel::deleteReceiver,
            replaceReceiversIfEmpty = viewModel::replaceReceiversIfEmpty,
            addProcessingMethod = viewModel::addProcessingMethod,
            deleteProcessingMethod = viewModel::deleteProcessingMethod,
            editProcessingMethod = viewModel::editProcessingMethod,
        )
    onStateReady(state)

    val pendingPrefill = uiState.pendingPrefill
    LaunchedEffect(pendingPrefill) {
        if (pendingPrefill != null) {
            state.applyFormPrefill(pendingPrefill)
            viewModel.onPrefillConsumed()
        }
    }

    val errorEvent = uiState.errorEvent

    AfternoteEditorScreen(
        form = uiState.form,
        onBackClick = {},
        onRegisterClick = {
            val form = state.currentForm()
            val payload =
                SaveAfternotePayloadBuilder.build(
                    form = form,
                    messageBlocks = state.currentEditorMessageBlocks(),
                    accountId = state.idState.text.toString(),
                    password = state.passwordState.text.toString(),
                    date = LocalDate.of(2026, 8, 22),
                )
            viewModel.saveAfternote(
                payload = payload,
                selectedReceiverIds = form.afternoteEditReceivers.map { it.id.toLong() },
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
        },
        snackbarMessage = null,
        validationMessage =
            (errorEvent?.error as? AfternoteEditorError.Validation)?.let {
                stringResource(it.reason.messageResId)
            },
        onSnackbarMessageConsumed = {
            errorEvent?.let(viewModel::onErrorConsumed)
        },
        onValidationMessageConsumed = {
            errorEvent?.let(viewModel::onErrorConsumed)
        },
        content = { editorSnackbarHostState ->
            AfternoteEditorBody(
                state = state,
                form = uiState.form,
                onNavigateToMemorialPlaylist = {},
                onNavigateToSelectReceiver = {},
                onThumbnailBytesReady = {},
                onThumbnailExtractionFailed = {},
                thumbnailRetryToken = 0,
                onCaptureFailed = {},
                snackbarHostState = editorSnackbarHostState,
                isPrefillLoading = uiState.isPrefillLoading,
            )
        },
        state = state,
        shouldDeferBaselineCapture = uiState.isPrefillLoading,
        snackbarMessageKey = errorEvent,
    )
}

@Composable
private fun AuthorDetailForDelete(
    viewModel: AfternoteDetailViewModel,
    deletedIds: MutableList<Long>,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val success = uiState as? AfternoteDetailUiState.Success
    LaunchedEffect(success?.deleteResult) {
        when (val result = success?.deleteResult) {
            is AfternoteDetailDeleteResult.Failed -> {
                val message =
                    resources.getString(
                        result.messageRes ?: AfternoteFeatureR.string.afternote_detail_delete_failed,
                    )
                scope.launch { snackbarHostState.showSnackbar(message) }
                viewModel.onDeleteResultConsumed()
            }

            is AfternoteDetailDeleteResult.Succeeded -> {
                deletedIds += result.id
                viewModel.onDeleteResultConsumed()
            }

            null -> {
                return@LaunchedEffect
            }
        }
    }

    when (val state = uiState) {
        AfternoteDetailUiState.Loading -> {
            CircularProgressIndicator()
        }

        is AfternoteDetailUiState.Error -> {
            Text("detail error")
        }

        is AfternoteDetailUiState.Success -> {
            val account = state.contentUiModel as DetailContentUiModel.SocialNetwork
            AccountDetailScreen(
                onBackClick = {},
                content = account.content,
                snackbarHostState = snackbarHostState,
                onEditClick = {},
                onDeleteConfirm = viewModel::deleteAfternote,
            )
        }
    }
}

private fun detailViewModel(
    repository: FakeAfternoteRepository,
    itemId: Long,
): AfternoteDetailViewModel =
    AfternoteDetailViewModel(
        route = AfternoteRoute.DetailRoute(itemId = itemId),
        afternoteRepository = repository,
        userRepository = afternoteAuthorUserRepository(),
        userProfileRepository = afternoteAuthorUserProfileRepository(),
        errorReporter = NoopAuthorErrorReporter,
    )

private fun editorViewModel(
    repository: FakeAfternoteRepository,
    itemId: Long,
): AfternoteEditorViewModel =
    AfternoteEditorViewModel(
        route =
            AfternoteRoute.EditorFlowRoute(
                itemId = itemId,
                initialType = AfternoteType.SOCIAL_NETWORK,
            ),
        savedStateHandle =
            afternoteEditorSavedStateHandle(
                initialType = AfternoteType.SOCIAL_NETWORK,
                itemId = itemId,
            ),
        userRepository = afternoteAuthorUserRepository(),
        afternoteRepository = repository,
        memorialThumbnailUploadRepository =
            MemorialThumbnailUploadRepository {
                Result.success("https://cdn.test/thumb.jpg")
            },
        resolveMemorialMediaForSave =
            ResolveMemorialMediaForSaveUseCase(
                memorialMediaUploadRepository =
                    MemorialMediaUploadRepository { input, kind ->
                        Result.success(
                            when (input) {
                                MediaInput.None -> {
                                    null
                                }

                                is MediaInput.Local -> {
                                    when (kind) {
                                        MediaKind.VIDEO -> "https://cdn.test/video.mp4"
                                        MediaKind.PHOTO -> "https://cdn.test/photo.jpg"
                                    }
                                }

                                is MediaInput.Remote -> {
                                    input.url
                                }
                            },
                        )
                    },
            ),
        errorReporter = NoopAuthorErrorReporter,
    )

private fun authorDetail(): Detail =
    Detail(
        id = 73L,
        serviceName = "Instagram",
        timestamps = DetailTimestamps(updatedAt = "2026.08.22"),
        receivers =
            listOf(
                DetailReceiver(
                    receiverId = 7L,
                    name = "김수신",
                    relation = "가족",
                ),
            ),
        leaveMessageBlocks =
            listOf(LeaveMessageBlock(title = "마지막 말", body = "기억해 줘")),
        content =
            DetailContent.SocialNetwork(
                credentials = DetailCredentials(id = "old@example.test", password = "old-password"),
                processingMethods = listOf("계정 삭제"),
            ),
    )
