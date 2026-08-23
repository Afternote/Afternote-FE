package com.afternote.afternote_fe

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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.afternote_fe.test.FakeUserRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.model.author.ProcessingMethod
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MemorialPhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialVideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.PhotoUploadOutcome
import com.afternote.feature.afternote.domain.repository.author.VideoUploadOutcome
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailDeleteResult
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.author.detail.DetailContentUiModel
import com.afternote.feature.afternote.presentation.author.detail.account.AccountDetailScreen
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreen
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternotePayloadBuilder
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.rememberAfternoteEditorState
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntry
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AfternoteAuthorExtendedAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun home_loadingErrorRetrySuccess_filterAndRoutesStayConnectedToRepository() {
        val firstLoadStarted = CompletableDeferred<Unit>()
        val releaseFirstLoad = CompletableDeferred<Unit>()
        val pagingSource =
            RetryListPagingSource(
                firstLoadStarted = firstLoadStarted,
                releaseFirstLoad = releaseFirstLoad,
                successItems = authorListItems(),
            )
        val repository = AdvancedAfternoteRepository()
        repository.listFlows[null] =
            Pager(PagingConfig(pageSize = 20)) { pagingSource }.flow
        repository.listFlows[AfternoteType.SOCIAL_NETWORK] = flowOf(PagingData.empty())
        val viewModel = AfternoteHomeViewModel(repository)
        val accountRoutes = mutableListOf<String>()
        val galleryRoutes = mutableListOf<String>()
        val memorialRoutes = mutableListOf<String>()
        val addRoutes = mutableListOf<AfternoteType?>()

        composeRule.setContent {
            AfternoteTheme {
                AfternoteHomeEntry(
                    navigateToDetail = accountRoutes::add,
                    navigateToGalleryDetail = galleryRoutes::add,
                    navigateToMemorialDetail = memorialRoutes::add,
                    navigateToAdd = addRoutes::add,
                    onSettingClick = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { firstLoadStarted.isCompleted }
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()

        releaseFirstLoad.complete(Unit)
        composeRule.onNodeWithText("애프터노트 목록을 불러오지 못했습니다.").assertIsDisplayed()
        assertEquals(1, pagingSource.loadCalls.get())

        composeRule.onNodeWithText("다시 시도").performClick()
        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
        assertEquals(2, pagingSource.loadCalls.get())

        composeRule.onNodeWithText("Instagram").performClick()
        composeRule.onNodeWithText("Google Drive").performScrollTo().performClick()
        composeRule
            .onNodeWithContentDescription("추억 노트")
            .performScrollTo()
            .performClick()
        assertEquals(listOf("101"), accountRoutes)
        assertEquals(listOf("102"), galleryRoutes)
        assertEquals(listOf("103"), memorialRoutes)

        composeRule.onNodeWithText("소셜네트워크").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.requestedTypes.lastOrNull() == AfternoteType.SOCIAL_NETWORK
        }
        composeRule.onNodeWithText("소셜네트워크").assertIsSelected()
        composeRule
            .onNodeWithText("해당 카테고리에 등록된 애프터노트가 없어요.")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("추가").performClick()

        assertEquals(listOf(AfternoteType.SOCIAL_NETWORK), addRoutes)
        repository.listFlows[null] = flowOf(PagingData.empty())
        composeRule.onNodeWithText("전체").performClick()
        composeRule
            .onNodeWithText("아직 등록된 답변이 없어요.\n답변을 등록해 자신을 알아 보아요.")
            .assertIsDisplayed()
        assertEquals(
            listOf(null, AfternoteType.SOCIAL_NETWORK, null),
            repository.requestedTypes,
        )
    }

    @Test
    fun detailEdit_updatesExactPrefilledPayloadThroughEditorRoute() {
        val repository = AdvancedAfternoteRepository(detailResult = Result.success(authorDetail()))
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
            checkNotNull(editorViewModel).editProcessingMethod("1", "계정 보존")
        }
        composeRule.onNodeWithText("계정 보존").performScrollTo().assertIsDisplayed()
        val topBarRegister =
            hasText("등록", substring = false) and
                hasClickAction() and
                SemanticsMatcher("without button role") { node ->
                    node.config.getOrNull(SemanticsProperties.Role) != Role.Button
                }
        composeRule.onNode(topBarRegister).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { repository.updateCalls.size == 1 }
        val (updatedId, payload) = repository.updateCalls.single()
        assertEquals(73L, updatedId)
        assertEquals("SOCIAL", payload.category)
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
        assertEquals(listOf(73L, 73L), repository.detailCalls)
        assertEquals("73", routedItemId)
    }

    @Test
    fun delete_cancelThenFailureAndRetrySuccess_callsOnlyAfterConfirmation() {
        val repository = AdvancedAfternoteRepository(detailResult = Result.success(authorDetail()))
        repository.deleteResults.addLast(Result.failure(IllegalStateException("offline")))
        repository.deleteResults.addLast(Result.success(Unit))
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
        assertEquals(emptyList<Long>(), repository.deleteCalls)
        composeRule.onNodeWithText("취소하기").performClick()
        assertEquals(emptyList<Long>(), repository.deleteCalls)

        openDeleteDialog()
        composeRule.onNodeWithText("삭제하기").performClick()
        composeRule.onNodeWithText("삭제에 실패했습니다.").assertIsDisplayed()
        assertEquals(listOf(73L), repository.deleteCalls)
        assertEquals(emptyList<Long>(), deletedIds)

        openDeleteDialog()
        composeRule.onNodeWithText("삭제하기").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { deletedIds == listOf(73L) }

        assertEquals(listOf(73L, 73L), repository.deleteCalls)
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
            val account = state.contentUiModel as DetailContentUiModel.Account
            AccountDetailScreen(
                onBackClick = {},
                content = account.content,
                onEditClick = { onEdit(state.detailId.toString()) },
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
            setCategory = viewModel::setCategory,
            setService = viewModel::setService,
            setMemorialPhoto = viewModel::setMemorialPhoto,
            setMemorialVideo = viewModel::setMemorialVideo,
            addReceiverIfAbsent = viewModel::addReceiverIfAbsent,
            applyPrefill = viewModel::applyPrefill,
            setMemorialThumbnail = viewModel::setMemorialThumbnail,
            setMemorialPlaylistSongs = viewModel::setMemorialPlaylistSongs,
            deleteReceiver = viewModel::deleteReceiver,
            replaceReceiversIfEmpty = viewModel::replaceReceiversIfEmpty,
            setLeaveMessageBlocks = viewModel::setLeaveMessageBlocks,
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

    AfternoteEditorScreen(
        form = uiState.form,
        onBackClick = {},
        onRegisterClick = {
            state.setLeaveMessageBlocks(
                state.editorMessages.map { message ->
                    EditorMessageTextBlock(
                        title = message.titleState.text.toString(),
                        body = message.contentState.text.toString(),
                    )
                },
            )
            val form = state.currentForm()
            val payload =
                SaveAfternotePayloadBuilder.build(
                    form = form,
                    accountId = state.idState.text.toString(),
                    password = state.passwordState.text.toString(),
                    date = LocalDate.of(2026, 8, 22),
                )
            viewModel.saveAfternote(
                editingId = itemId,
                category = form.selectedCategory,
                payload = payload,
                selectedReceiverIds = form.afternoteEditReceivers.map { it.id.toLong() },
                playlistSongs = emptyList(),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
        },
        onNavigateToMemorialPlaylist = {},
        onNavigateToSelectReceiver = {},
        onThumbnailBytesReady = {},
        onThumbnailExtractionFailed = {},
        onThumbnailUploadErrorConsumed = viewModel::onThumbnailUploadErrorConsumed,
        onValidationErrorConsumed = viewModel::onValidationErrorConsumed,
        state = state,
        isPrefillLoading = uiState.isPrefillLoading,
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
                        result.messageRes ?: R.string.afternote_detail_delete_failed,
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
            val account = state.contentUiModel as DetailContentUiModel.Account
            AccountDetailScreen(
                onBackClick = {},
                content = account.content,
                snackbarHostState = snackbarHostState,
                onDeleteConfirm = { viewModel.deleteAfternote(state.detailId) },
            )
        }
    }
}

private class RetryListPagingSource(
    private val firstLoadStarted: CompletableDeferred<Unit>,
    private val releaseFirstLoad: CompletableDeferred<Unit>,
    private val successItems: List<ListItem>,
) : PagingSource<Int, ListItem>() {
    val loadCalls = AtomicInteger()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListItem> {
        val attempt = loadCalls.incrementAndGet()
        return if (attempt == 1) {
            firstLoadStarted.complete(Unit)
            releaseFirstLoad.await()
            LoadResult.Error(IllegalStateException("offline"))
        } else {
            LoadResult.Page(
                data = successItems,
                prevKey = null,
                nextKey = null,
            )
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ListItem>): Int? = null
}

private class AdvancedAfternoteRepository(
    var detailResult: Result<Detail> = Result.failure(NoSuchElementException()),
) : AfternoteRepository {
    val listFlows = mutableMapOf<AfternoteType?, Flow<PagingData<ListItem>>>()
    val requestedTypes = mutableListOf<AfternoteType?>()
    val detailCalls = mutableListOf<Long>()
    val updateCalls = mutableListOf<Pair<Long, AfternoteUpdatePayload>>()
    val deleteCalls = mutableListOf<Long>()
    val deleteResults = ArrayDeque<Result<Unit>>()

    override fun getPagedAfternotes(type: AfternoteType?): Flow<PagingData<ListItem>> {
        requestedTypes += type
        return listFlows[type] ?: flowOf(PagingData.empty())
    }

    override suspend fun getDetail(id: Long): Result<Detail> {
        detailCalls += id
        return detailResult
    }

    override suspend fun createSocial(payload: CreateAccountPayload): Result<Long> = error("unexpected createSocial")

    override suspend fun createBusiness(payload: CreateAccountPayload): Result<Long> = error("unexpected createBusiness")

    override suspend fun createGallery(payload: CreateGalleryPayload): Result<Long> = error("unexpected createGallery")

    override suspend fun createMemorial(payload: CreateMemorialPayload): Result<Long> = error("unexpected createMemorial")

    override suspend fun update(
        id: Long,
        payload: AfternoteUpdatePayload,
    ): Result<Long> {
        updateCalls += id to payload
        return Result.success(id)
    }

    override suspend fun delete(id: Long): Result<Unit> {
        deleteCalls += id
        return deleteResults.removeFirstOrNull() ?: Result.success(Unit)
    }
}

private fun detailViewModel(
    repository: AdvancedAfternoteRepository,
    itemId: Long,
): AfternoteDetailViewModel =
    AfternoteDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("itemId" to itemId.toString())),
        afternoteRepository = repository,
        userRepository = FakeUserRepository(),
        errorReporter = FakeErrorReporter(),
    )

private fun editorViewModel(
    repository: AdvancedAfternoteRepository,
    itemId: Long,
): AfternoteEditorViewModel =
    AfternoteEditorViewModel(
        savedStateHandle = SavedStateHandle(mapOf("itemId" to itemId.toString())),
        userRepository = FakeUserRepository(),
        afternoteRepository = repository,
        memorialThumbnailUploadRepository =
            MemorialThumbnailUploadRepository {
                Result.success("https://cdn.test/thumb.jpg")
            },
        resolveMemorialMediaForSave =
            ResolveMemorialMediaForSaveUseCase(
                memorialVideoUploadRepository =
                    MemorialVideoUploadRepository { input ->
                        Result.success(
                            when (input) {
                                MediaInput.None -> {
                                    VideoUploadOutcome.Empty
                                }

                                is MediaInput.Local -> {
                                    VideoUploadOutcome.FreshlyUploaded("https://cdn.test/video.mp4")
                                }

                                is MediaInput.Remote -> {
                                    VideoUploadOutcome.Existing(input.url)
                                }
                            },
                        )
                    },
                memorialPhotoUploadRepository =
                    MemorialPhotoUploadRepository { input ->
                        Result.success(
                            when (input) {
                                MediaInput.None -> {
                                    PhotoUploadOutcome.Empty
                                }

                                is MediaInput.Local -> {
                                    PhotoUploadOutcome.FreshlyUploaded("https://cdn.test/photo.jpg")
                                }

                                is MediaInput.Remote -> {
                                    PhotoUploadOutcome.Existing(input.url)
                                }
                            },
                        )
                    },
            ),
        errorReporter = FakeErrorReporter(),
    )

private fun authorListItems(): List<ListItem> =
    listOf(
        ListItem(
            id = "101",
            serviceName = "Instagram",
            date = "2026.08.22",
            type = AfternoteType.SOCIAL_NETWORK,
        ),
        ListItem(
            id = "102",
            serviceName = "Google Drive",
            date = "2026.08.22",
            type = AfternoteType.GALLERY_AND_FILES,
        ),
        ListItem(
            id = "103",
            serviceName = "추억 노트",
            date = "2026.08.22",
            type = AfternoteType.MEMORIAL,
        ),
    )

private fun authorDetail(): Detail =
    Detail(
        id = 73L,
        category = "SOCIAL",
        title = "Instagram",
        timestamps = DetailTimestamps(createdAt = "2026.08.20", updatedAt = "2026.08.22"),
        type = AfternoteType.SOCIAL_NETWORK,
        credentials = DetailCredentials(id = "old@example.test", password = "old-password"),
        receivers =
            listOf(
                DetailReceiver(
                    receiverId = 7L,
                    name = "김수신",
                    relation = "가족",
                    phone = "",
                ),
            ),
        processingMethods = listOf("계정 삭제"),
        leaveMessageBlocks =
            listOf(LeaveMessageBlock(title = "마지막 말", body = "기억해 줘")),
        memorial = null,
    )
