package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.paging.PagingData
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedExportBundle
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MasterKeyViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `UUID 형식이 아닌 마스터 키는 사용자 오류를 표시하고 검증 API를 호출하지 않는다`() {
        val authRepository = RecordingReceiverAuthRepository()
        val receiverRepository = RecordingReceiverRepository()
        val viewModel = viewModel(authRepository, receiverRepository)

        listOf("wrong-key-0000", "1-1-1-1-1").forEach { invalidMasterKey ->
            viewModel.submit(senderId = "sender-id", authCode = invalidMasterKey)

            assertEquals(
                UiText.Resource(R.string.receiver_verify_master_key_invalid_format),
                viewModel.uiState.value.error,
            )
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

        assertTrue(authRepository.verifiedMasterKeys.isEmpty())
        assertTrue(receiverRepository.savedAuthCodes.isEmpty())
    }

    @Test
    fun `정규 UUID 마스터 키는 공백을 제거해 검증하고 저장한다`() {
        val authRepository = RecordingReceiverAuthRepository()
        val receiverRepository = RecordingReceiverRepository()
        val senderRegistry = SenderRegistry()
        val sender = senderRegistry.register("별칭")
        val masterKey = "123e4567-e89b-12d3-a456-426614174000"
        val viewModel = viewModel(authRepository, receiverRepository, senderRegistry)

        viewModel.submit(senderId = sender.id, authCode = "  $masterKey\n")

        assertEquals(listOf(masterKey), authRepository.verifiedMasterKeys)
        assertEquals(listOf(masterKey), receiverRepository.savedAuthCodes)
        assertEquals("발신자", senderRegistry.findById(sender.id)?.realSenderName)
        assertTrue(viewModel.uiState.value.isVerified)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `대문자 마스터 키는 소문자로 정규화해 검증하고 저장한다`() {
        val authRepository = RecordingReceiverAuthRepository()
        val receiverRepository = RecordingReceiverRepository()
        val senderRegistry = SenderRegistry()
        val sender = senderRegistry.register("별칭")
        val viewModel = viewModel(authRepository, receiverRepository, senderRegistry)

        viewModel.submit(senderId = sender.id, authCode = "123E4567-E89B-12D3-A456-426614174000")

        val normalized = "123e4567-e89b-12d3-a456-426614174000"
        assertEquals(listOf(normalized), authRepository.verifiedMasterKeys)
        assertEquals(listOf(normalized), receiverRepository.savedAuthCodes)
        assertTrue(viewModel.uiState.value.isVerified)
        assertEquals(null, viewModel.uiState.value.error)
    }

    private fun viewModel(
        authRepository: RecordingReceiverAuthRepository,
        receiverRepository: RecordingReceiverRepository,
        senderRegistry: SenderRegistry = SenderRegistry(),
    ): MasterKeyViewModel =
        MasterKeyViewModel(
            senderRegistry = senderRegistry,
            receiverRepository = receiverRepository,
            receiverAuthRepository = authRepository,
            errorReporter = NoopErrorReporter,
        )

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }
}

private class RecordingReceiverAuthRepository : ReceiverAuthRepository {
    val verifiedMasterKeys = mutableListOf<String>()

    override suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity> {
        verifiedMasterKeys += authCode
        return Result.success(
            ReceiverIdentity(
                receiverId = 1L,
                receiverName = "수신자",
                senderName = "발신자",
                relation = "가족",
            ),
        )
    }

    override suspend fun sendEmailAuthCode(email: String): Result<Unit> = error("호출되면 안 됨")

    override suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult> = error("호출되면 안 됨")

    override suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl> = error("호출되면 안 됨")

    override suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification> = error("호출되면 안 됨")

    override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> = error("호출되면 안 됨")

    override suspend fun getSenderMessage(): Result<SenderMessageInfo> = error("호출되면 안 됨")
}

private class RecordingReceiverRepository : ReceiverRepository {
    private val storedAuthCode = MutableStateFlow<String?>(null)

    val savedAuthCodes = mutableListOf<String>()

    override val authCodeFlow: Flow<String?> = storedAuthCode

    override suspend fun currentAuthCode(): String? = storedAuthCode.value

    override suspend fun saveAuthCode(code: String) {
        savedAuthCodes += code
        storedAuthCode.value = code
    }

    override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> = flowOf(PagingData.empty())

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> = error("호출되면 안 됨")

    override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> = error("호출되면 안 됨")

    override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> = error("호출되면 안 됨")

    override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = error("호출되면 안 됨")

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> = error("호출되면 안 됨")
}
