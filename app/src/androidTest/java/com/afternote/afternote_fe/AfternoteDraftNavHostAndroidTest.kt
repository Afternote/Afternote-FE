package com.afternote.afternote_fe

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.HiltTestActivity
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.data.di.AfternoteAuthorRepositoryModule
import com.afternote.feature.afternote.data.repositoryimpl.author.MemorialAudioUploadRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.author.MemorialMediaUploadRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.author.MemorialThumbnailUploadRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.author.MusicSearchRepositoryImpl
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.DraftDetail
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialAudioUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MusicSearchRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.presentation.navigation.AfternoteExternalActions
import com.afternote.feature.afternote.presentation.navigation.AfternoteNavHost
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the production host and Hilt factories; no test access to internal route or ViewModel types. */
@HiltAndroidTest
@UninstallModules(AfternoteAuthorRepositoryModule::class)
@RunWith(AndroidJUnit4::class)
class AfternoteDraftNavHostAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule = FailureArtifactRule { composeRule.onRoot().captureToImage().asAndroidBitmap() }

    private val fake = FakeAfternoteRepository()

    @BindValue
    @JvmField
    val repository: AfternoteRepository = fake

    private lateinit var restorationTester: StateRestorationTester

    @Before
    fun setUp() {
        hiltRule.inject()
        // Static PagingData.from has no load-state completion by default. Model the completed
        // repository response explicitly so the empty published list can leave its loading state.
        fake.onGetPagedAfternotes = { type ->
            completedPage(fake.items.filter { !it.isDraft && (type == null || it.type == type) })
        }
        fake.onGetPagedDrafts = { type ->
            completedPage(fake.items.filter { it.isDraft && (type == null || it.type == type) })
        }
        fake.items +=
            ListItem(id = DRAFT_ID, serviceName = DRAFT_TITLE, date = "2026.09.08", type = AfternoteType.GALLERY_AND_FILES, isDraft = true)
        fake.draftDetails[DRAFT_ID] = draft()
    }

    @Test
    fun draftList_resumeAndRestoreRecreatesAssistedDraftRoute_thenBackReturnsToListAndHome() {
        launchHost()
        openDraft()
        assertEquals(listOf(DRAFT_ID), fake.requestedDraftDetailIds.toList())
        assertTrue(fake.requestedDetailIds.isEmpty())
        composeRule.onNodeWithText("임시저장").assertIsEnabled()
        composeRule.onNodeWithText("등록").assertIsEnabled()

        // Clear retained ViewModels as well as restoring saved composition state so a fresh assisted
        // factory must receive the restored draft key; an existing ViewModel cannot mask a lost flag.
        composeRule.runOnIdle { composeRule.activity.viewModelStore.clear() }
        restorationTester.emulateSavedInstanceStateRestore()
        waitForText("등록")
        composeRule.waitUntil(5_000) { fake.requestedDraftDetailIds.size == 2 }
        composeRule.onNodeWithText(DRAFT_TITLE).assertIsDisplayed()
        assertTrue(fake.requestedDetailIds.isEmpty())

        back()
        waitForText("임시 저장된 애프터노트")
        composeRule.onNodeWithText(DRAFT_TITLE).assertIsDisplayed()
        back()
        waitForText("임시저장")
        composeRule.onNodeWithText("임시 저장된 애프터노트").assertDoesNotExist()
    }

    @Test
    fun activityRecreationRetainsUnsubmittedDraftInputAndEditorViewModel() {
        fake.items[0] = fake.items[0].copy(type = AfternoteType.SOCIAL_NETWORK)
        fake.draftDetails[DRAFT_ID] = draft().copy(type = AfternoteType.SOCIAL_NETWORK)
        composeRule.activityRule.scenario.onActivity(::setActivityHostContent)
        waitForText("임시저장")
        openDraft()
        composeRule
            .onNodeWithText("아이디를 입력해 주세요.")
            .performScrollTo()
            .performTextInput("still-writing")

        composeRule.activityRule.scenario.recreate()
        composeRule.activityRule.scenario.onActivity(::setActivityHostContent)
        waitForText("still-writing")
        composeRule.onNodeWithText("still-writing").assertIsDisplayed()
        // Configuration recreation retains the flow ViewModel and does not refetch stale prefill.
        assertEquals(listOf(DRAFT_ID), fake.requestedDraftDetailIds.toList())
        assertTrue(fake.requestedDetailIds.isEmpty())
        back()
        waitForText("나가기")
        composeRule.onNodeWithText("나가기").performClick()
        waitForText("임시 저장된 애프터노트")
    }

    @Test
    fun draftSaveKeepsDraft_thenPublishClearsFlagAndReturnsHome() {
        launchHost()
        openDraft()
        composeRule.onNodeWithText("임시저장").performClick()
        composeRule.waitUntil(5_000) { fake.updateCalls.size == 1 }
        waitForText("임시저장")
        assertEquals(DRAFT_ID, fake.updateCalls.single().first)
        assertEquals(
            true,
            fake.updateCalls
                .single()
                .second.isDraft,
        )

        // A complete draft can be published through the same real editor flow.
        fake.draftDetails[DRAFT_ID] =
            draft().copy(
                processingMethods = listOf("계정을 그대로 유지해 주세요"),
                receivers = listOf(DetailReceiver(receiverId = 7L, name = "김수신", relation = "딸")),
            )
        openDraft()
        composeRule.onNodeWithText("등록").performClick()
        composeRule.waitUntil(5_000) { fake.updateCalls.size == 2 }
        assertEquals(DRAFT_ID, fake.updateCalls.last().first)
        assertEquals(
            false,
            fake.updateCalls
                .last()
                .second.isDraft,
        )
        waitForText("임시저장")
        composeRule.onNodeWithText("등록").assertDoesNotExist()
    }

    @Test
    fun homeFilterLoadingAndRetryKeepDraftEntryActionable() {
        val firstFilterLoad = CompletableDeferred<Unit>()
        val retryFilterLoad = CompletableDeferred<Unit>()
        var filterLoads = 0
        fake.onGetPagedAfternotes = { type ->
            if (type == null) {
                completedPage(emptyList())
            } else {
                Pager(PagingConfig(pageSize = 20)) {
                    object : PagingSource<Int, ListItem>() {
                        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListItem> {
                            filterLoads += 1
                            return if (filterLoads == 1) {
                                firstFilterLoad.await()
                                LoadResult.Error(IllegalStateException("offline"))
                            } else {
                                retryFilterLoad.await()
                                LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
                            }
                        }

                        override fun getRefreshKey(state: PagingState<Int, ListItem>): Int? = null
                    }
                }.flow
            }
        }
        launchHost()
        waitForText("임시저장")
        try {
            composeRule.mainClock.autoAdvance = false
            composeRule.onNodeWithText("소셜네트워크").performClick()
            composeRule.mainClock.advanceTimeBy(500)
            composeRule
                .onNodeWithText("임시저장")
                .assertIsDisplayed()
                .assertIsEnabled()
                .performClick()
            composeRule.mainClock.advanceTimeBy(500)
            composeRule.onNodeWithText("임시 저장된 애프터노트").assertIsDisplayed()

            back()
            composeRule.mainClock.advanceTimeBy(500)
            firstFilterLoad.complete(Unit)
            composeRule.mainClock.autoAdvance = true
            waitForText("다시 시도")

            composeRule.mainClock.autoAdvance = false
            composeRule.onNodeWithText("다시 시도").performClick()
            composeRule.mainClock.advanceTimeBy(500)
            composeRule.onNodeWithText("다시 시도").assertDoesNotExist()
            composeRule
                .onNodeWithText("임시저장")
                .assertIsDisplayed()
                .assertIsEnabled()
                .performClick()
            composeRule.mainClock.advanceTimeBy(500)
            composeRule.onNodeWithText("임시 저장된 애프터노트").assertIsDisplayed()
            composeRule.waitUntil(5_000) { filterLoads == 2 }
            assertEquals(listOf<AfternoteType?>(null, null), fake.requestedDraftTypes.toList())
            assertEquals(AfternoteType.SOCIAL_NETWORK, fake.requestedTypes.last())
        } finally {
            firstFilterLoad.complete(Unit)
            retryFilterLoad.complete(Unit)
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun draftPrefillFailureDisablesSaving_untilRetryUsesDraftContract() {
        val releaseLoad = CompletableDeferred<Unit>()
        var attempts = 0
        fake.onGetDraftDetail = {
            attempts += 1
            if (attempts == 1) {
                releaseLoad.await()
                Result.failure(IllegalStateException("offline"))
            } else {
                Result.success(draft())
            }
        }
        launchHost()
        composeRule.onNodeWithText("임시저장").performClick()
        waitForText(DRAFT_TITLE)
        // Hold the animated prefill skeleton on a known frame; waiting for Compose idleness
        // while the repository is deliberately suspended would wait for the shimmer forever.
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithText(DRAFT_TITLE).performClick()
            composeRule.mainClock.advanceTimeBy(500)
            composeRule.onNodeWithText("등록").assertIsNotEnabled()
            composeRule.onNodeWithText("임시저장").assertIsNotEnabled()
        } finally {
            releaseLoad.complete(Unit)
            composeRule.mainClock.autoAdvance = true
        }
        waitForText("다시 불러오기")
        composeRule.onNodeWithText("등록").assertIsNotEnabled()
        composeRule.onNodeWithText("임시저장").assertIsNotEnabled()
        composeRule.onNodeWithText("다시 불러오기").performClick()
        waitForText(DRAFT_TITLE)
        composeRule.onNodeWithText("등록").assertIsEnabled()
        composeRule.onNodeWithText("임시저장").assertIsEnabled()
        assertEquals(listOf(DRAFT_ID, DRAFT_ID), fake.requestedDraftDetailIds.toList())
        assertTrue(fake.requestedDetailIds.isEmpty())
        assertTrue(fake.updateCalls.isEmpty())
    }

    @Test
    fun draftList_refreshErrorCanRetryToEmpty_thenBackReturnsHome() {
        var attempts = 0
        fake.onGetPagedDrafts = {
            Pager(PagingConfig(pageSize = 20)) {
                object : PagingSource<Int, ListItem>() {
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListItem> {
                        attempts += 1
                        return if (attempts == 1) {
                            LoadResult.Error(IllegalStateException("offline"))
                        } else {
                            LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
                        }
                    }

                    override fun getRefreshKey(state: PagingState<Int, ListItem>): Int? = null
                }
            }.flow
        }
        launchHost()
        composeRule.onNodeWithText("임시저장").performClick()
        waitForText("다시 시도")
        composeRule.onNodeWithText("다시 시도").performClick()
        waitForText(composeRule.activity.getString(com.afternote.feature.afternote.presentation.R.string.afternote_draft_list_empty_body))
        assertEquals(2, attempts)
        assertEquals(listOf<AfternoteType?>(null), fake.requestedDraftTypes.toList())
        back()
        waitForText("임시저장")
    }

    @Test
    fun publishedEditDoesNotOfferDraftSave() {
        val publishedId = 81L
        fake.items += ListItem(id = publishedId, serviceName = "발행한 기록", date = "2026.09.08", type = AfternoteType.GALLERY_AND_FILES)
        fake.details[publishedId] =
            Detail(
                id = publishedId,
                serviceName = "발행한 기록",
                timestamps = DetailTimestamps(updatedAt = "2026-09-08"),
                receivers = emptyList(),
                leaveMessageBlocks = emptyList(),
                content = DetailContent.Gallery(processingMethods = listOf("계정을 그대로 유지해 주세요")),
            )
        launchHost()
        composeRule.onNodeWithText("발행한 기록").performClick()
        composeRule.onNodeWithContentDescription("수정").performClick()
        waitForText("수정하기")
        composeRule.onNodeWithText("수정하기").performClick()
        waitForText("등록")
        composeRule.onNodeWithText("임시저장").assertDoesNotExist()
        assertTrue(fake.requestedDraftDetailIds.isEmpty())
        assertTrue(fake.requestedDetailIds.all { it == publishedId })
    }

    private fun setActivityHostContent(activity: HiltTestActivity) {
        // The same composition call site is required for saveable keys on both Activity instances.
        activity.setContent { HostContent() }
    }

    private fun completedPage(items: List<ListItem>) =
        flowOf(
            PagingData.from(
                items,
                sourceLoadStates =
                    LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = true),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true),
                    ),
            ),
        )

    private fun launchHost() {
        restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { HostContent() }
        waitForText("임시저장")
    }

    @Composable
    private fun HostContent() {
        AfternoteTheme {
            AfternoteNavHost(
                boundary = FeatureStackBoundary { error("Unexpected host exit") },
                externalActions =
                    object : AfternoteExternalActions {
                        override fun navigateToBottomTab(tab: BottomNavTab) = error("Unexpected tab")

                        override fun navigateToSetting() = error("Unexpected setting")

                        override fun onFingerprintAuthFailed(message: String) = error(message)
                    },
            )
        }
    }

    private fun openDraft() {
        composeRule.onNodeWithText("임시저장").performClick()
        waitForText("임시 저장된 애프터노트")
        waitForText(DRAFT_TITLE)
        val titleBounds = composeRule.onNodeWithText("임시 저장된 애프터노트").fetchSemanticsNode().boundsInRoot
        val itemBounds = composeRule.onNodeWithText(DRAFT_TITLE).fetchSemanticsNode().boundsInRoot
        assertTrue("Draft rows must be laid out below the navigation bar", itemBounds.top >= titleBounds.bottom)
        composeRule.onNodeWithText(DRAFT_TITLE).performClick()
        waitForText("등록")
        waitForText(DRAFT_TITLE)
    }

    private fun back() = composeRule.onNodeWithContentDescription("뒤로가기").performClick()

    private fun waitForText(text: String) {
        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText(text),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun draft() =
        DraftDetail(
            id = DRAFT_ID,
            type = AfternoteType.GALLERY_AND_FILES,
            serviceName = DRAFT_TITLE,
            timestamps = DetailTimestamps(updatedAt = "2026-09-08"),
            receivers = emptyList(),
            leaveMessageBlocks = emptyList(),
            credentials = null,
            processingMethods = emptyList(),
            songs = emptyList(),
            media = MemorialMedia(photoUrl = null, videoUrl = null, thumbnailUrl = null, audioUrl = null),
        )

    @Module
    @InstallIn(SingletonComponent::class)
    object UnusedAuthorBindings {
        @Provides
        fun music(impl: MusicSearchRepositoryImpl): MusicSearchRepository = impl

        @Provides
        fun thumbnail(impl: MemorialThumbnailUploadRepositoryImpl): MemorialThumbnailUploadRepository = impl

        @Provides
        fun audio(impl: MemorialAudioUploadRepositoryImpl): MemorialAudioUploadRepository = impl

        @Provides
        fun media(impl: MemorialMediaUploadRepositoryImpl): MemorialMediaUploadRepository = impl
    }

    private companion object {
        const val DRAFT_ID = 71L
        const val DRAFT_TITLE = "이어서 작성할 사진 기록"
    }
}
