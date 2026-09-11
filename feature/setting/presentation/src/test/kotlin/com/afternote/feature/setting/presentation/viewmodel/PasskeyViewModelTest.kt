package com.afternote.feature.setting.presentation.viewmodel

import androidx.credentials.exceptions.CreateCredentialCancellationException
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.domain.PasskeyRepository
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = PasskeyScenario()
    private val cache = FakeUserProfileCacheRepository()
    private val reporter = PasskeyFailureRecorder()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun registration_waitsForServerBeforeUpdatingCacheOrReturningSuccess() =
        runTest(dispatcher) {
            val server = CompletableDeferred<Passkey>()
            repository.register = { credential ->
                assertEquals("platform-credential", credential)
                server.await()
            }
            val viewModel = PassKeyViewModel(cache, repository, reporter)
            viewModel.onIntent(
                PassKeyIntent.Register { options ->
                    assertEquals("server-options", options)
                    "platform-credential"
                },
            )
            runCurrent()
            assertTrue(viewModel.uiState.value.isRegistering)
            assertEquals(null, viewModel.uiState.value.result)
            assertTrue(cache.savedPasskeyValues.isEmpty())
            server.complete(PASSKEY)
            runCurrent()
            assertSame(PasskeyRegistrationResult.Success, viewModel.uiState.value.result)
            assertEquals(listOf(true), cache.savedPasskeyValues)
            assertTrue(reporter.stages.isEmpty())
        }

    @Test
    fun registration_optionsFailureDoesNotLaunchCredentialManagerOrSaveCache() =
        runTest(dispatcher) {
            repository.options = { throw IOException("private server response") }
            val result =
                register(PassKeyViewModel(cache, repository, reporter)) {
                    error("Credential Manager must not run after options failure")
                }
            assertEquals(
                PasskeyRegistrationResult.Error(UiText.Resource(R.string.setting_passkey_options_error)),
                result,
            )
            assertEquals(listOf("passkey_registration_options"), reporter.stages)
            assertTrue(cache.savedPasskeyValues.isEmpty())
            assertTrue(repository.credentials.isEmpty())
        }

    @Test
    fun registration_unexpectedCredentialResponseIsReportedWithoutRegistering() =
        runTest(dispatcher) {
            val result =
                register(PassKeyViewModel(cache, repository, reporter)) {
                    error("Unexpected credential response")
                }
            assertEquals(
                PasskeyRegistrationResult.Error(UiText.Resource(R.string.setting_passkey_registration_error)),
                result,
            )
            assertEquals(listOf("passkey_create_credential"), reporter.stages)
            assertTrue(repository.credentials.isEmpty())
            assertTrue(cache.savedPasskeyValues.isEmpty())
        }

    @Test
    fun registration_nativeCancellationIsQuietAndDoesNotRegister() =
        runTest(dispatcher) {
            val result =
                register(PassKeyViewModel(cache, repository, reporter)) {
                    throw CreateCredentialCancellationException()
                }
            assertSame(PasskeyRegistrationResult.Canceled, result)
            assertTrue(reporter.stages.isEmpty())
            assertTrue(repository.credentials.isEmpty())
            assertTrue(cache.savedPasskeyValues.isEmpty())
        }

    @Test
    fun registration_coroutineCancellationPropagatesWithoutReporting() =
        runTest(dispatcher) {
            val cancellation = CancellationException("screen left")
            repository.options = { throw cancellation }
            val viewModel = PassKeyViewModel(cache, repository, reporter)
            viewModel.onIntent(PassKeyIntent.Register { "unused" })
            runCurrent()
            assertFalse(viewModel.uiState.value.isRegistering)
            assertEquals(null, viewModel.uiState.value.result)
            assertTrue(reporter.stages.isEmpty())
            assertTrue(cache.savedPasskeyValues.isEmpty())
        }

    @Test
    fun registration_serverFailureIsReportedWithoutSavingSuccess() =
        runTest(dispatcher) {
            repository.register = { throw IOException("server rejected credential") }
            val result = register(PassKeyViewModel(cache, repository, reporter)) { "credential" }
            assertTrue(result is PasskeyRegistrationResult.Error)
            assertEquals(listOf("passkey_register"), reporter.stages)
            assertTrue(cache.savedPasskeyValues.isEmpty())
        }

    @Test
    fun registration_cacheFailureKeepsServerSuccessAndReportsOnlyCacheStage() =
        runTest(dispatcher) {
            cache.onSavePasskeyRegistered = { throw IOException("disk") }
            val result = register(PassKeyViewModel(cache, repository, reporter)) { "credential" }
            assertSame(PasskeyRegistrationResult.Success, result)
            assertEquals(listOf("credential"), repository.credentials)
            assertEquals(listOf("passkey_registration_cache"), reporter.stages)
        }

    @Test
    fun list_failureIsDistinctFromEmptyAndRetryCanRecover() =
        runTest(dispatcher) {
            repository.list = { throw IOException("offline") }
            val viewModel = PassKeyListViewModel(repository, reporter)
            viewModel.onIntent(PassKeyListIntent.Refresh)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(UiText.Resource(R.string.setting_passkey_list_error), viewModel.uiState.value.errorMessage)
            assertEquals(listOf("passkey_list"), reporter.stages)

            repository.list = { emptyList() }
            viewModel.onIntent(PassKeyListIntent.Refresh)
            advanceUntilIdle()
            assertEquals(PassKeyListUiState(), viewModel.uiState.value)

            repository.list = { listOf(PASSKEY) }
            viewModel.onIntent(PassKeyListIntent.Refresh)
            advanceUntilIdle()
            assertEquals(listOf(PASSKEY), viewModel.uiState.value.passkeys)
        }

    @Test
    fun list_repeatedRefreshDuringRequestDoesNotDuplicateServerCall() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<List<Passkey>>()
            var calls = 0
            repository.list = {
                calls++
                gate.await()
            }
            val viewModel = PassKeyListViewModel(repository, reporter)
            viewModel.onIntent(PassKeyListIntent.Refresh)
            runCurrent()
            viewModel.onIntent(PassKeyListIntent.Refresh)
            runCurrent()
            assertEquals(1, calls)
            gate.complete(listOf(PASSKEY))
            advanceUntilIdle()
            assertEquals(listOf(PASSKEY), viewModel.uiState.value.passkeys)
        }

    @Test
    fun registration_cancelThenRetryIgnoresPreviousCompletionAndBlocksDuplicateSubmission() =
        runTest(dispatcher) {
            val first = CompletableDeferred<String>()
            val second = CompletableDeferred<String>()
            val viewModel = PassKeyViewModel(cache, repository, reporter)
            viewModel.onIntent(PassKeyIntent.Register { first.await() })
            runCurrent()
            viewModel.onIntent(PassKeyIntent.CancelRegistration)
            viewModel.onIntent(PassKeyIntent.Register { second.await() })
            viewModel.onIntent(PassKeyIntent.Register { error("duplicate platform request") })
            runCurrent()
            assertTrue(viewModel.uiState.value.isRegistering)
            first.complete("stale")
            runCurrent()
            assertTrue(repository.credentials.isEmpty())
            second.complete("new")
            runCurrent()
            assertEquals(listOf("new"), repository.credentials)
            assertSame(PasskeyRegistrationResult.Success, viewModel.uiState.value.result)
            viewModel.onIntent(PassKeyIntent.Register { error("success must be consumed first") })
            runCurrent()
            assertEquals(listOf("new"), repository.credentials)
            viewModel.onIntent(PassKeyIntent.ConsumeResult(PasskeyRegistrationResult.Success))
            assertEquals(null, viewModel.uiState.value.result)
            viewModel.onIntent(PassKeyIntent.Register { "again" })
            runCurrent()
            assertSame(PasskeyRegistrationResult.Success, viewModel.uiState.value.result)
            assertEquals(listOf("new", "again"), repository.credentials)
        }

    private fun TestScope.register(
        viewModel: PassKeyViewModel,
        createCredential: suspend (String) -> String,
    ): PasskeyRegistrationResult? {
        viewModel.onIntent(PassKeyIntent.Register(createCredential))
        runCurrent()
        return viewModel.uiState.value.result
    }
}

private class PasskeyScenario : PasskeyRepository {
    var list: suspend () -> List<Passkey> = { emptyList() }
    var options: suspend () -> String = { "server-options" }
    var register: suspend (String) -> Passkey = { PASSKEY }
    val credentials = mutableListOf<String>()

    override suspend fun getPasskeys(): List<Passkey> = list()

    override suspend fun getRegistrationOptions(): String = options()

    override suspend fun registerPasskey(credentialJson: String): Passkey {
        credentials += credentialJson
        return register(credentialJson)
    }
}

private class PasskeyFailureRecorder : ErrorReporter {
    val stages = mutableListOf<String>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        stages += requireNotNull(attributes["stage"])
        assertFalse(throwable.message.orEmpty().contains("private server response"))
    }
}

private val PASSKEY = Passkey(id = 7L, displayName = "Test passkey", createdAt = "2026-09-06T10:00:00")
