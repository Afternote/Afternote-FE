package com.afternote.feature.receiver.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 열람 신청 흐름의 단계 소거 회귀 기준 (#1601 · #1698).
 *
 * 인증번호·마스터 키는 서버가 이미 소비했으므로, 각 단계를 통과하면 지나온 화면이 스택에
 * 남으면 안 된다. Nav2 의 `popUpTo(inclusive = true)` 가 하던 일을 로컬 스택이 그대로 한다.
 */
class DeliveryVerificationFlowLocalNavActionsTest {
    private var exits = 0
    private var exitsToRecords = 0
    private val stepStack = NavBackStack<NavKey>(ReceiverRoute.IdentityVerificationIntroRoute)
    private val actions =
        DeliveryVerificationFlowLocalNavActions(
            stepStack = stepStack,
            boundary = FeatureStackBoundary { exits += 1 },
            onExitToReceivedRecords = { exitsToRecords += 1 },
        )

    private fun stack(): List<String> = stepStack.map { it::class.simpleName!! }

    @Test
    fun `마스터 키로 넘어가면 본인 확인 화면들이 남지 않는다`() {
        actions.navigateToIdentityVerificationEmail()
        assertEquals(
            listOf("IdentityVerificationIntroRoute", "IdentityVerificationEmailRoute"),
            stack(),
        )

        actions.proceedToMasterKey()

        assertEquals(listOf("MasterKeyRoute"), stack())
    }

    @Test
    fun `본인 확인 캐시로 안내를 건너뛴 경로도 같은 모양이 된다`() {
        actions.proceedToMasterKey()

        assertEquals(listOf("MasterKeyRoute"), stack())
    }

    @Test
    fun `서류 업로드와 완료도 직전 단계를 남기지 않는다`() {
        actions.proceedToMasterKey()

        actions.proceedToDocumentUpload()
        assertEquals(listOf("DocumentUploadRoute"), stack())

        actions.proceedToDeliveryVerificationComplete()
        assertEquals(listOf("DeliveryVerificationCompleteRoute"), stack())
    }

    @Test
    fun `단계 소거 뒤 뒤로가기는 흐름을 통째로 벗어난다`() {
        actions.proceedToMasterKey()

        actions.popBack()

        // 흐름 스택엔 되돌아갈 단계가 남아 있지 않으므로 흐름 entry 자체가 내려간다.
        assertEquals(listOf("MasterKeyRoute"), stack())
        assertEquals(1, exits)
    }

    @Test
    fun `완료 화면의 받은 기록함으로는 바깥 스택이 처리한다`() {
        actions.proceedToDeliveryVerificationComplete()

        actions.popToReceivedRecords()

        assertEquals(1, exitsToRecords)
    }
}
