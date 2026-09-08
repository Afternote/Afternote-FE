package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 소스 선택 시트의 열림/닫힘과 선택 라우팅 (#369).
 *
 * 시트 표시 여부를 [MemorialMediaSourceState.target] 하나로 표현한 덕에, "떠 있는 슬롯" 과
 * "결과를 받을 슬롯" 이 어긋날 수 없다는 것을 여기서 확인한다.
 */
class MemorialMediaSourceStateTest {
    private val galleryCalls = mutableListOf<MemorialMediaTarget>()
    private val captureCalls = mutableListOf<MemorialMediaTarget>()
    private val removeCalls = mutableListOf<MemorialMediaTarget>()
    private val state =
        MemorialMediaSourceState(
            openTarget = mutableStateOf(null),
            onPickFromGallery = galleryCalls::add,
            onCapture = captureCalls::add,
            onRemove = removeCalls::add,
        )

    @Test
    fun `기본은 닫힘이다`() {
        assertNull(state.target)
    }

    @Test
    fun `연 슬롯이 그대로 시트 대상이 된다`() {
        state.open(MemorialMediaTarget.VIDEO)

        assertEquals(MemorialMediaTarget.VIDEO, state.target)
    }

    @Test
    fun `갤러리를 고르면 시트를 닫고 그 슬롯으로 넘긴다`() {
        state.open(MemorialMediaTarget.PHOTO)

        state.pickFromGallery()

        assertEquals(listOf(MemorialMediaTarget.PHOTO), galleryCalls)
        assertNull(state.target)
    }

    @Test
    fun `촬영을 고르면 시트를 닫고 그 슬롯으로 넘긴다`() {
        state.open(MemorialMediaTarget.VIDEO)

        state.capture()

        assertEquals(listOf(MemorialMediaTarget.VIDEO), captureCalls)
        assertNull(state.target)
    }

    @Test
    fun `삭제를 고르면 시트를 닫고 그 슬롯으로 넘긴다`() {
        state.open(MemorialMediaTarget.PHOTO)

        state.remove()

        assertEquals(listOf(MemorialMediaTarget.PHOTO), removeCalls)
        assertNull(state.target)
    }

    @Test
    fun `닫힌 상태의 선택은 인텐트를 쏘지 않는다`() {
        // 시트가 사라지는 애니메이션 도중 들어온 탭이 인텐트를 두 번 쏘는 것을 막는다.
        state.pickFromGallery()
        state.capture()
        state.remove()

        assertEquals(emptyList<MemorialMediaTarget>(), galleryCalls)
        assertEquals(emptyList<MemorialMediaTarget>(), captureCalls)
        assertEquals(emptyList<MemorialMediaTarget>(), removeCalls)
    }

    @Test
    fun `바깥을 눌러 닫으면 아무 슬롯도 선택되지 않는다`() {
        state.open(MemorialMediaTarget.PHOTO)

        state.dismiss()

        assertNull(state.target)
        assertEquals(emptyList<MemorialMediaTarget>(), galleryCalls)
        assertEquals(emptyList<MemorialMediaTarget>(), captureCalls)
    }
}
