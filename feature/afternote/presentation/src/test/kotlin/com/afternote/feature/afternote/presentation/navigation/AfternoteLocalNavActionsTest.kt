package com.afternote.feature.afternote.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 애프터노트 작성자 로컬 백스택 경계 회귀 기준 (#1601 · #1698).
 *
 * 지문 관문은 이 스택의 **시작 화면**이다 — 인증을 지나면 관문이 남으면 안 되고, 홈에서의
 * 뒤로가기는 관문이 아니라 그래프 밖으로 나가야 한다. Nav2 시절 `AuthBoundaryBackStackTest`
 * 와 `BottomTabBackStackRestorationTest` 가 루트 백스택으로 보던 몫이다.
 */
class AfternoteLocalNavActionsTest {
    private var exits = 0
    private val backStack = NavBackStack<NavKey>(AfternoteRoute.FingerprintLoginRoute)
    private val external = RecordingExternalActions()
    private val actions =
        AfternoteLocalNavActions(
            backStack = backStack,
            boundary = FeatureStackBoundary { exits += 1 },
            externalActions = external,
        )

    private fun stack(): List<String> = backStack.map { it::class.simpleName!! }

    @Test
    fun `스택은 지문 관문에서 시작한다`() {
        assertEquals(listOf("FingerprintLoginRoute"), stack())
    }

    @Test
    fun `지문 관문은 인증 성공 뒤 백스택에서 사라진다`() {
        actions.replaceFingerprintLoginWithAfternoteHome()

        assertEquals(listOf("AfternoteHomeRoute"), stack())

        // 애프터노트 홈에서 뒤로가기는 지문 관문이 아니라 그래프 밖으로 나간다.
        actions.popBack()
        assertEquals(listOf("AfternoteHomeRoute"), stack())
        assertEquals(1, exits)
    }

    @Test
    fun `상세와 에디터 흐름은 홈 위에 쌓인다`() {
        actions.replaceFingerprintLoginWithAfternoteHome()

        actions.navigateToAfternoteDetail(itemId = 7L)
        actions.navigateToEditorForEdit(itemId = 7L, initialType = AfternoteType.MEMORIAL)

        assertEquals(
            listOf("AfternoteHomeRoute", "DetailRoute", "EditorFlowRoute"),
            stack(),
        )
    }

    @Test
    fun `에디터 진입 키가 수정 대상과 종류를 나른다`() {
        actions.navigateToEditorForEdit(itemId = 42L, initialType = AfternoteType.MEMORIAL)
        val edit = backStack.last() as AfternoteRoute.EditorFlowRoute
        assertEquals(42L, edit.itemId)
        assertEquals(AfternoteType.MEMORIAL, edit.initialType)

        actions.navigateToNewEditor(initialType = AfternoteType.SOCIAL_NETWORK)
        val create = backStack.last() as AfternoteRoute.EditorFlowRoute
        assertEquals(null, create.itemId)
        assertEquals(AfternoteType.SOCIAL_NETWORK, create.initialType)
    }

    @Test
    fun `홈에서 임시저장 목록으로 이동하고 뒤로가기로 홈에 돌아온다`() {
        actions.replaceFingerprintLoginWithAfternoteHome()

        actions.navigateToDraftList()
        assertEquals(listOf("AfternoteHomeRoute", "DraftListRoute"), stack())

        actions.popBack()
        assertEquals(listOf("AfternoteHomeRoute"), stack())
    }

    @Test
    fun `임시저장 목록에서 고른 항목은 이어쓰기로 에디터에 이어진다`() {
        actions.replaceFingerprintLoginWithAfternoteHome()
        actions.navigateToDraftList()

        actions.navigateToEditorForResume(itemId = 31L, initialType = AfternoteType.SOCIAL_NETWORK)

        assertEquals(
            listOf("AfternoteHomeRoute", "DraftListRoute", "EditorFlowRoute"),
            stack(),
        )
        // 이어쓰기를 그만두면 목록으로 돌아온다.
        actions.popBack()
        assertEquals(listOf("AfternoteHomeRoute", "DraftListRoute"), stack())
    }

    @Test
    fun `이어쓰기 진입은 isDraft 를 목적지까지 나른다`() {
        actions.navigateToEditorForResume(itemId = 31L, initialType = AfternoteType.MEMORIAL)

        val key = backStack.last() as AfternoteRoute.EditorFlowRoute
        assertEquals(31L, key.itemId)
        assertEquals(AfternoteType.MEMORIAL, key.initialType)
        // 발행 보장이 없는 draft 상세로 읽어야 한다 — 이 표식이 빠지면 이어쓰기가 발행 계약으로
        // 파싱돼 프리필이 실패한다 (#808).
        assertEquals(true, key.isDraft)
    }

    @Test
    fun `상세에서 온 수정 진입은 발행 계약을 그대로 받는다`() {
        actions.navigateToEditorForEdit(itemId = 31L, initialType = AfternoteType.MEMORIAL)

        assertEquals(false, (backStack.last() as AfternoteRoute.EditorFlowRoute).isDraft)
    }

    @Test
    fun `저장 성공은 홈 위 화면들을 걷어내고 홈만 남긴다`() {
        actions.replaceFingerprintLoginWithAfternoteHome()
        actions.navigateToAfternoteDetail(itemId = 7L)
        actions.navigateToNewEditor(initialType = AfternoteType.MEMORIAL)

        actions.popToAfternoteHome()

        assertEquals(listOf("AfternoteHomeRoute"), stack())
    }

    @Test
    fun `그래프 밖 이동은 스택을 건드리지 않고 셸로 나간다`() {
        actions.navigateToSetting()
        actions.navigateToBottomTab(BottomNavTab.HOME)
        actions.onFingerprintAuthFailed("지문 인증에 실패했습니다")

        assertTrue(external.wentToSetting)
        assertEquals(BottomNavTab.HOME, external.tab)
        assertEquals("지문 인증에 실패했습니다", external.fingerprintError)
        assertEquals(listOf("FingerprintLoginRoute"), stack())
    }

    private class RecordingExternalActions : AfternoteExternalActions {
        var wentToSetting = false
        var tab: BottomNavTab? = null
        var fingerprintError: String? = null

        override fun navigateToBottomTab(tab: BottomNavTab) {
            this.tab = tab
        }

        override fun navigateToSetting() {
            wentToSetting = true
        }

        override fun onFingerprintAuthFailed(message: String) {
            fingerprintError = message
        }
    }
}
