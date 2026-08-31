package com.afternote.feature.afternote.presentation.receiver.afternotelist

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService
import com.afternote.feature.afternote.presentation.shared.util.getIconResForType
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 수신 목록 카드가 발신자가 고른 서비스명을 보여준다는 계약 (이슈 #617, #753).
 *
 * 알려진 서비스 아이콘은 이름이, 카탈로그 밖 fallback과 필터 탭은 종류가 결정한다.
 */
class ReceiverAfternoteListMappingTest {
    private fun listItem(
        serviceName: String,
        type: AfternoteType = AfternoteType.SOCIAL_NETWORK,
    ) = AfterNoteListItem(
        id = 5,
        serviceName = serviceName,
        type = type,
        lastUpdatedAt = "2026.07.29",
    )

    @Test
    fun `카드 주 텍스트는 서버가 준 서비스명이다`() {
        val uiModel = listItem(serviceName = "인스타그램").toUiModel()

        assertEquals(5L, uiModel.id)
        assertEquals("인스타그램", uiModel.serviceName)
    }

    @Test
    fun `어떤 종류에서도 enum 이름이 카드 주 텍스트로 새지 않는다`() {
        AfternoteType.entries.forEach { type ->
            val uiModel = listItem(serviceName = "네이버 메일", type = type).toUiModel()

            assertNotEquals(
                "$type 카드에 종류 enum 이름이 그대로 노출됐다",
                type.name,
                uiModel.serviceName,
            )
        }
    }

    @Test
    fun `카탈로그에 있는 서비스명이면 그 서비스 아이콘을 쓴다`() {
        val uiModel =
            listItem(
                serviceName = AfternoteService.INSTAGRAM.displayKey,
                type = AfternoteType.SOCIAL_NETWORK,
            ).toUiModel()

        assertEquals(AfternoteService.INSTAGRAM.iconResId, uiModel.iconResId)
    }

    /** 카탈로그 밖 이름은 #490 이전에 저장된 "직접 추가하기" 데이터에서만 온다. */
    @Test
    fun `카탈로그에 없는 이름은 그 항목의 종류 아이콘으로 떨어진다`() {
        AfternoteType.entries.forEach { type ->
            val uiModel =
                listItem(
                    serviceName = "내가 직접 적은 서비스",
                    type = type,
                ).toUiModel()

            assertEquals(getIconResForType(type), uiModel.iconResId)
        }
    }
}
