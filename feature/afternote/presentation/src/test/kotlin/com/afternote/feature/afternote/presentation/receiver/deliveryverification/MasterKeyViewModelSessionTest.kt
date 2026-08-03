package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.paging.PagingData
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.session.UserSessionGuard
import com.afternote.core.domain.session.UserSessionStamp
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.receiver.recordsbox.SenderRegistry
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MasterKeyViewModelSessionTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `검증 대기 중 세션이 바뀌면 성공 응답이 이전 수신자 상태를 저장하지 않는다`() =
        runTest(dispatcher) {
            val senderRegistry = SenderRegistry()
            val sender = senderRegistry.register("이전 계정 발신자")
            val receiverRepository = RecordingReceiverRepository()
            val receiverAuthRepository = SuspendedReceiverAuthRepository()
            val sessionGuard = MutableUserSessionGuard()
            val viewModel =
                MasterKeyViewModel(
                    senderRegistry = senderRegistry,
                    receiverRepository = receiverRepository,
                    receiverAuthRepository = receiverAuthRepository,
                    errorReporter = FailingErrorReporter,
                    userSessionGuard = sessionGuard,
                )

            viewModel.submit(sender.id, " AUTH-CODE-A ")
            runCurrent()
            assertEquals(listOf("AUTH-CODE-A"), receiverAuthRepository.requestedCodes)

            sessionGuard.switchUser()
            receiverAuthRepository.completeSuccess(
                ReceiverIdentity(
                    receiverId = 1L,
                    receiverName = "이전 계정 수신자",
                    senderName = "이전 계정 실제 발신자",
                    relation = "친구",
                ),
            )
            advanceUntilIdle()

            assertTrue(receiverRepository.savedCodes.isEmpty())
            assertEquals(sender, senderRegistry.findById(sender.id))
            assertFalse(viewModel.uiState.value.isSubmitting)
            assertFalse(viewModel.uiState.value.isVerified)
        }
}

private class MutableUserSessionGuard : UserSessionGuard {
    private var current = UserSessionStamp(generation = 1, isActive = true)

    override suspend fun currentActive(): UserSessionStamp = current

    override suspend fun commitIfCurrent(
        expected: UserSessionStamp,
        block: suspend () -> Unit,
    ): Boolean {
        if (expected != current || !current.isActive) return false
        block()
        return true
    }

    fun switchUser() {
        current = UserSessionStamp(generation = current.generation + 1, isActive = true)
    }
}

private class SuspendedReceiverAuthRepository : ReceiverAuthRepository {
    private val verificationResult = CompletableDeferred<Result<ReceiverIdentity>>()
    val requestedCodes = mutableListOf<String>()

    override suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity> {
        requestedCodes += authCode
        return verificationResult.await()
    }

    fun completeSuccess(identity: ReceiverIdentity) {
        verificationResult.complete(Result.success(identity))
    }

    override suspend fun sendEmailAuthCode(email: String): Result<Unit> = error("미사용")

    override suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult> = error("미사용")

    override suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl> = error("미사용")

    override suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification> = error("미사용")

    override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> = error("미사용")

    override suspend fun getSenderMessage(): Result<SenderMessageInfo> = error("미사용")
}

private class RecordingReceiverRepository : ReceiverRepository {
    val savedCodes = mutableListOf<String>()

    override val authCodeFlow: Flow<String?> = emptyFlow()

    override suspend fun currentAuthCode(): String? = error("미사용")

    override suspend fun saveAuthCode(code: String) {
        savedCodes += code
    }

    override suspend fun clearAuthCode() = error("미사용")

    override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> = error("미사용")

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> = error("미사용")

    override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> = error("미사용")

    override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> = error("미사용")

    override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = error("미사용")

    override suspend fun loadMindRecordsCount(): Result<LoadCountResult> = error("미사용")

    override suspend fun loadTimeLettersCount(): Result<LoadCountResult> = error("미사용")

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> = error("미사용")
}

private object FailingErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = error("성공 응답은 실패로 기록되면 안 됨")
}
