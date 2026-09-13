package com.afternote.feature.afternote.presentation.receiver.afternotelist

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagingData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import com.afternote.core.ui.R as CoreUiR

/**
 * 수신 목록 카드가 발신자가 고른 서비스명을 보여준다는 계약 (이슈 #617, #753).
 *
 * 알려진 서비스 아이콘은 이름이, 카탈로그 밖 fallback과 필터 탭은 종류가 결정한다.
 * 판정은 화면이 실제로 소비하는 [ReceiverAfternoteHomeViewModel.pagedAfternotes] 의 결과로 한다 —
 * 목록 항목 매퍼는 그 안의 구현 세부다 (#1673).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverAfternoteListMappingTest {
    private val dispatcher = UnconfinedTestDispatcher()

    /** 카탈로그 밖 이름이 떨어지는 종류별 아이콘. 정본은 카테고리 아이콘 시안(4163:19696 외)이다. */
    private val categoryIconResByType =
        mapOf(
            AfternoteType.SOCIAL_NETWORK to CoreUiR.drawable.core_ui_afternote_social_pattern,
            AfternoteType.BUSINESS to CoreUiR.drawable.core_ui_afternote_business_pattern,
            AfternoteType.ESTATE to CoreUiR.drawable.core_ui_afternote_business_pattern,
            AfternoteType.GALLERY_AND_FILES to CoreUiR.drawable.core_ui_afternote_gallery_category_pattern,
            AfternoteType.MEMORIAL to CoreUiR.drawable.core_ui_afternote_memorial_guideline,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `카드 주 텍스트는 서버가 준 서비스명이다`() {
        val uiModel = uiModelsOf(listItem(serviceName = "인스타그램")).single()

        assertEquals(5L, uiModel.id)
        assertEquals("인스타그램", uiModel.serviceName)
    }

    @Test
    fun `어떤 종류에서도 enum 이름이 카드 주 텍스트로 새지 않는다`() {
        val uiModels = uiModelsOf(*AfternoteType.entries.map { listItem("네이버 메일", it) }.toTypedArray())

        assertEquals(AfternoteType.entries.size, uiModels.size)
        uiModels.forEach { uiModel ->
            assertNotEquals(
                "${uiModel.type} 카드에 종류 enum 이름이 그대로 노출됐다",
                uiModel.type.name,
                uiModel.serviceName,
            )
        }
    }

    @Test
    fun `카탈로그에 있는 서비스명이면 그 서비스 아이콘을 쓴다`() {
        val uiModel =
            uiModelsOf(
                listItem(
                    serviceName = AfternoteService.INSTAGRAM.displayKey,
                    type = AfternoteType.SOCIAL_NETWORK,
                ),
            ).single()

        assertEquals(AfternoteService.INSTAGRAM.iconResId, uiModel.iconResId)
    }

    /** 카탈로그 밖 이름은 #490 이전에 저장된 "직접 추가하기" 데이터에서만 온다. */
    @Test
    fun `카탈로그에 없는 이름은 그 항목의 종류 아이콘으로 떨어진다`() {
        val uiModels = uiModelsOf(*AfternoteType.entries.map { listItem("내가 직접 적은 서비스", it) }.toTypedArray())

        assertEquals(AfternoteType.entries.size, uiModels.size)
        uiModels.forEach { uiModel ->
            assertEquals(categoryIconResByType.getValue(uiModel.type), uiModel.iconResId)
        }
    }

    private fun listItem(
        serviceName: String,
        type: AfternoteType = AfternoteType.SOCIAL_NETWORK,
    ) = AfterNoteListItem(
        id = 5,
        serviceName = serviceName,
        type = type,
        lastUpdatedAt = "2026.07.29",
    )

    /** ViewModel 이 화면에 흘리는 [PagingData] 를 그대로 받아 UI 모델 목록으로 편다. */
    private fun uiModelsOf(vararg items: AfterNoteListItem): List<ListItemUiModel> {
        lateinit var result: List<ListItemUiModel>
        runTest(dispatcher) {
            val viewModel =
                ReceiverAfternoteHomeViewModel(
                    FakeReceiverRepository(pagedAfterNotes = flowOf(PagingData.from(items.toList()))),
                )
            val differ =
                AsyncPagingDataDiffer(
                    diffCallback = UI_MODEL_DIFF,
                    updateCallback = NoopListUpdateCallback,
                    mainDispatcher = dispatcher,
                    workerDispatcher = dispatcher,
                )
            val collection = launch { viewModel.pagedAfternotes.collect(differ::submitData) }
            advanceUntilIdle()
            result = differ.snapshot().items
            collection.cancel()
        }
        return result
    }

    private companion object {
        val UI_MODEL_DIFF =
            object : DiffUtil.ItemCallback<ListItemUiModel>() {
                override fun areItemsTheSame(
                    oldItem: ListItemUiModel,
                    newItem: ListItemUiModel,
                ): Boolean = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: ListItemUiModel,
                    newItem: ListItemUiModel,
                ): Boolean = oldItem == newItem
            }

        val NoopListUpdateCallback =
            object : ListUpdateCallback {
                override fun onInserted(
                    position: Int,
                    count: Int,
                ) = Unit

                override fun onRemoved(
                    position: Int,
                    count: Int,
                ) = Unit

                override fun onMoved(
                    fromPosition: Int,
                    toPosition: Int,
                ) = Unit

                override fun onChanged(
                    position: Int,
                    count: Int,
                    payload: Any?,
                ) = Unit
            }
    }
}
