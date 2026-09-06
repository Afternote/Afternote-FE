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
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
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
            val registration =
                async {
                    viewModel.registerPasskey { options ->
                        assertEquals("server-options", options)
                        "platform-credential"
                    }
                }
            runCurrent()
            assertFalse(registration.isCompleted)
            assertTrue(cache.savedPasskeyValues.isEmpty())
            server.complete(PASSKEY)
            assertSame(PasskeyRegistrationResult.Success, registration.await())
            assertEquals(listOf(true), cache.savedPasskeyValues)
            assertTrue(reporter.stages.isEmpty())
        }

    @Test
    fun registration_optionsFailureDoesNotLaunchCredentialManagerOrSaveCache() =
        runTest(dispatcher) {
            repository.options = { throw IOException("private server response") }
            val result =
                PassKeyViewModel(cache, repository, reporter).registerPasskey {
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
                PassKeyViewModel(cache, repository, reporter).registerPasskey {
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
                PassKeyViewModel(cache, repository, reporter).registerPasskey {
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
            try {
                PassKeyViewModel(cache, repository, reporter).registerPasskey { "unused" }
                error("Cancellation must propagate")
            } catch (failure: CancellationException) {
                assertSame(cancellation, failure)
            }
            assertTrue(reporter.stages.isEmpty())
            assertTrue(cache.savedPasskeyValues.isEmpty())
        }

    @Test
    fun registration_serverFailureIsReportedWithoutSavingSuccess() =
        runTest(dispatcher) {
            repository.register = { throw IOException("server rejected credential") }
            val result = PassKeyViewModel(cache, repository, reporter).registerPasskey { "credential" }
            assertTrue(result is PasskeyRegistrationResult.Error)
            assertEquals(listOf("passkey_register"), reporter.stages)
            assertTrue(cache.savedPasskeyValues.isEmpty())
        }

    @Test
    fun registration_cacheFailureKeepsServerSuccessAndReportsOnlyCacheStage() =
        runTest(dispatcher) {
            cache.onSavePasskeyRegistered = { throw IOException("disk") }
            val result = PassKeyViewModel(cache, repository, reporter).registerPasskey { "credential" }
            assertSame(PasskeyRegistrationResult.Success, result)
            assertEquals(listOf("credential"), repository.credentials)
            assertEquals(listOf("passkey_registration_cache"), reporter.stages)
        }

    @Test
    fun list_failureIsDistinctFromEmptyAndRetryCanRecover() =
        runTest(dispatcher) {
            repository.list = { throw IOException("offline") }
            val viewModel = PassKeyListViewModel(repository, reporter)
            viewModel.refresh()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(UiText.Resource(R.string.setting_passkey_list_error), viewModel.uiState.value.errorMessage)
            assertEquals(listOf("passkey_list"), reporter.stages)

            repository.list = { emptyList() }
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(PassKeyListUiState(), viewModel.uiState.value)

            repository.list = { listOf(PASSKEY) }
            viewModel.refresh()
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
            viewModel.refresh()
            runCurrent()
            viewModel.refresh()
            runCurrent()
            assertEquals(1, calls)
            gate.complete(listOf(PASSKEY))
            advanceUntilIdle()
            assertEquals(listOf(PASSKEY), viewModel.uiState.value.passkeys)
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
