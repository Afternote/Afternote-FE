package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.domain.model.SenderEntry
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.receiver.domain.testing.FakeSenderRegistryRepository
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
        val authRepository = fakeReceiverAuthRepository()
        val receiverRepository = fakeReceiverRepository()
        val viewModel = viewModel(authRepository, receiverRepository)

        listOf("wrong-key-0000", "1-1-1-1-1").forEach { invalidMasterKey ->
            viewModel.submit(senderId = "sender-id", masterKey = invalidMasterKey)

            assertEquals(
                UiText.Resource(R.string.receiver_verify_master_key_invalid_format),
                viewModel.uiState.value.errorMessage,
            )
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

        assertTrue(authRepository.verifiedMasterKeys.isEmpty())
        assertTrue(receiverRepository.savedMasterKeys.isEmpty())
    }

    @Test
    fun `정규 UUID 마스터 키는 공백을 제거해 검증하고 저장한다`() {
        val authRepository = fakeReceiverAuthRepository()
        val receiverRepository = fakeReceiverRepository()
        val sender = SenderEntry(id = "sender-id", name = "별칭")
        val senderRegistry = senderRegistry(listOf(sender))
        val masterKey = "123e4567-e89b-12d3-a456-426614174000"
        val viewModel = viewModel(authRepository, receiverRepository, senderRegistry)

        viewModel.submit(senderId = sender.id, masterKey = "  $masterKey\n")

        assertEquals(listOf(masterKey), authRepository.verifiedMasterKeys)
        assertEquals(listOf(masterKey), receiverRepository.savedMasterKeys)
        assertEquals(
            "발신자",
            runBlocking { senderRegistry.findById(sender.id).getOrThrow()?.realSenderName },
        )
        assertTrue(viewModel.uiState.value.isVerified)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `대문자 마스터 키는 소문자로 정규화해 검증하고 저장한다`() {
        val authRepository = fakeReceiverAuthRepository()
        val receiverRepository = fakeReceiverRepository()
        val sender = SenderEntry(id = "sender-id", name = "별칭")
        val senderRegistry = senderRegistry(listOf(sender))
        val viewModel = viewModel(authRepository, receiverRepository, senderRegistry)

        viewModel.submit(senderId = sender.id, masterKey = "123E4567-E89B-12D3-A456-426614174000")

        val normalized = "123e4567-e89b-12d3-a456-426614174000"
        assertEquals(listOf(normalized), authRepository.verifiedMasterKeys)
        assertEquals(listOf(normalized), receiverRepository.savedMasterKeys)
        assertTrue(viewModel.uiState.value.isVerified)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `마스터 키 검증 후 카드 저장이 실패하면 검증 완료로 이동하지 않는다`() {
        val authRepository = fakeReceiverAuthRepository()
        val receiverRepository = fakeReceiverRepository()
        val sender = SenderEntry(id = "sender-id", name = "별칭")
        val registryRepository =
            FakeSenderRegistryRepository(initialSenders = listOf(sender)).apply {
                onAttachIdentity = { _, _, _ -> Result.failure(IllegalStateException("write failed")) }
            }
        val viewModel = viewModel(authRepository, receiverRepository, SenderRegistry(registryRepository))

        viewModel.submit(
            senderId = sender.id,
            masterKey = "123e4567-e89b-12d3-a456-426614174000",
        )

        assertFalse(viewModel.uiState.value.isVerified)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(
            UiText.Resource(R.string.receiver_verify_error_unknown),
            viewModel.uiState.value.error,
        )
    }

    private fun viewModel(
        authRepository: FakeReceiverAuthRepository,
        receiverRepository: FakeReceiverRepository,
        senderRegistry: SenderRegistry = senderRegistry(),
    ): MasterKeyViewModel =
        MasterKeyViewModel(
            senderRegistry = senderRegistry,
            receiverRepository = receiverRepository,
            receiverAuthRepository = authRepository,
            errorReporter = NoopErrorReporter,
        )

    private fun senderRegistry(initialSenders: List<SenderEntry> = emptyList()): SenderRegistry =
        SenderRegistry(FakeSenderRegistryRepository(initialSenders = initialSenders))

    private fun fakeReceiverAuthRepository(): FakeReceiverAuthRepository =
        FakeReceiverAuthRepository.strict().apply {
            onVerifyMasterKey = { Result.success(identity) }
        }

    private fun fakeReceiverRepository(): FakeReceiverRepository =
        FakeReceiverRepository.strict().apply {
            onSaveMasterKey = { code -> masterKeyState.value = code }
        }

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }
}
