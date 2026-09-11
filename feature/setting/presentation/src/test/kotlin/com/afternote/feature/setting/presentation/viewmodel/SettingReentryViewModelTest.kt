package com.afternote.feature.setting.presentation.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.presentation.NoOpErrorReporter
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsIntent
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionIntent
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingReentryViewModelTest {
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
    fun connectedAccounts_firstResumeAndPendingLoadDoNotDuplicateAndRefreshKeepsContent() =
        runTest(dispatcher) {
            var calls = 0
            var response = CompletableDeferred<UserConnectedAccount>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetConnectedAccounts = {
                        calls++
                        response.await()
                    }
                }
            val viewModel = ConnectedAccountsViewModel(repository)
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(1, calls)
            response.complete(accounts())
            runCurrent()
            val previous = viewModel.uiState.value

            response = CompletableDeferred()
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(2, calls)
            assertEquals(previous, viewModel.uiState.value)
            response.complete(accounts(google = true))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.accounts
                    .single { it.provider == "google" }
                    .isConnected,
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun connectedAccounts_failedRefreshPreservesLoadedState() =
        runTest(dispatcher) {
            val repository = FakeUserRepository.strict().apply { onGetConnectedAccounts = { accounts() } }
            val viewModel = ConnectedAccountsViewModel(repository)
            runCurrent()
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            val previous = viewModel.uiState.value
            repository.onGetConnectedAccounts = { error("offline") }
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(previous, viewModel.uiState.value)
        }

    @Test
    fun connectedAccounts_successfulReentryClearsInitialLoadError() =
        runTest(dispatcher) {
            val repository = FakeUserRepository.strict().apply { onGetConnectedAccounts = { error("offline") } }
            val viewModel = ConnectedAccountsViewModel(repository)
            runCurrent()
            assertNotNull(viewModel.uiState.value.errorMessage)
            repository.onGetConnectedAccounts = { accounts() }
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            runCurrent()
            assertNull(viewModel.uiState.value.errorMessage)
            assertEquals(4, viewModel.uiState.value.accounts.size)
        }

    @Test
    fun connectedAccounts_linkCancelsStaleReadAndResumeWaitsForMutation() =
        runTest(dispatcher) {
            var reads = 0
            val stale = CompletableDeferred<UserConnectedAccount>()
            val linked = CompletableDeferred<UserConnectedAccount>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetConnectedAccounts = { if (++reads == 1) accounts() else stale.await() }
                    onLinkConnectedAccount = { _, _ -> linked.await() }
                }
            val viewModel = ConnectedAccountsViewModel(repository)
            runCurrent()
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            runCurrent()
            viewModel.onIntent(ConnectedAccountsIntent.Link("google", "token"))
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(2, reads)
            linked.complete(accounts(google = true))
            runCurrent()
            stale.complete(accounts())
            runCurrent()
            assertTrue(
                viewModel.uiState.value.accounts
                    .single { it.provider == "google" }
                    .isConnected,
            )
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun connectedAccounts_unlinkCancelsStaleReadAndResumeWaitsForMutation() =
        runTest(dispatcher) {
            var reads = 0
            val stale = CompletableDeferred<UserConnectedAccount>()
            val unlinked = CompletableDeferred<UserConnectedAccount>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetConnectedAccounts = { if (++reads == 1) accounts(google = true) else stale.await() }
                    onUnlinkConnectedAccount = { unlinked.await() }
                }
            val viewModel = ConnectedAccountsViewModel(repository)
            runCurrent()
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            runCurrent()
            viewModel.onIntent(ConnectedAccountsIntent.Toggle("google", false))
            viewModel.onIntent(ConnectedAccountsIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(2, reads)
            unlinked.complete(accounts())
            runCurrent()
            stale.complete(accounts(google = true))
            runCurrent()
            assertFalse(
                viewModel.uiState.value.accounts
                    .single { it.provider == "google" }
                    .isConnected,
            )
        }

    @Test
    fun delivery_firstResumeAndPendingLoadDoNotDuplicateAndRefreshKeepsContent() =
        runTest(dispatcher) {
            var calls = 0
            var response = CompletableDeferred<ReceiverDeliveryConditions>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetReceiverDeliveryConditions = {
                        calls++
                        response.await()
                    }
                }
            val viewModel = deliveryViewModel(repository)
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(1, calls)
            response.complete(delivery())
            runCurrent()
            val previous = viewModel.uiState.value
            response = CompletableDeferred()
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(2, calls)
            assertEquals(previous, viewModel.uiState.value)
            response.complete(delivery(DeliveryConditionType.RECEIVER_REQUEST))
            runCurrent()
            assertEquals(DeliveryConditionType.RECEIVER_REQUEST, viewModel.uiState.value.conditionType)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun delivery_refreshPreservesUnsavedSelectionAndSavesItWithRefreshedConditions() =
        runTest(dispatcher) {
            val response = CompletableDeferred<ReceiverDeliveryConditions>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetReceiverDeliveryConditions = { delivery() }
                    onUpdateReceiverDeliveryConditions = { id, conditions -> ReceiverDeliveryConditions(id, conditions) }
                }
            val viewModel = deliveryViewModel(repository)
            runCurrent()
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            viewModel.onIntent(DeliveryConditionIntent.SelectConditionType(1))
            repository.onGetReceiverDeliveryConditions = { response.await() }
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(DeliveryConditionType.RECEIVER_REQUEST, viewModel.uiState.value.conditionType)
            val refreshed =
                delivery().let {
                    it.copy(
                        conditions =
                            it.conditions + it.conditions.single().copy(contentType = DeliveryContentType.DIARY),
                    )
                }
            response.complete(refreshed)
            runCurrent()
            assertEquals(DeliveryConditionType.RECEIVER_REQUEST, viewModel.uiState.value.conditionType)
            viewModel.onIntent(DeliveryConditionIntent.Save)
            runCurrent()
            val saved = repository.deliveryUpdateCalls.single().conditions
            assertEquals(DeliveryConditionType.RECEIVER_REQUEST, saved.first().conditionType)
            assertNull(saved.first().inactivityPeriod)
            assertEquals(refreshed.conditions.last(), saved.last())
        }

    @Test
    fun delivery_editWhileRefreshIsPendingIsNotOverwritten() =
        runTest(dispatcher) {
            val response = CompletableDeferred<ReceiverDeliveryConditions>()
            val repository = FakeUserRepository.strict().apply { onGetReceiverDeliveryConditions = { delivery() } }
            val viewModel = deliveryViewModel(repository)
            runCurrent()
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            repository.onGetReceiverDeliveryConditions = { response.await() }
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            runCurrent()
            viewModel.onIntent(DeliveryConditionIntent.SelectConditionType(1))
            response.complete(delivery())
            runCurrent()
            assertEquals(DeliveryConditionType.RECEIVER_REQUEST, viewModel.uiState.value.conditionType)
        }

    @Test
    fun delivery_failedRefreshPreservesLoadedStateAndSuccessfulRetryClearsInitialError() =
        runTest(dispatcher) {
            val repository = FakeUserRepository.strict().apply { onGetReceiverDeliveryConditions = { error("offline") } }
            val viewModel = deliveryViewModel(repository)
            runCurrent()
            assertEquals(DeliveryConditionError.LOAD_FAILED, viewModel.uiState.value.error)
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            repository.onGetReceiverDeliveryConditions = { delivery() }
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            runCurrent()
            assertNull(viewModel.uiState.value.error)
            val previous = viewModel.uiState.value
            repository.onGetReceiverDeliveryConditions = { error("offline") }
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(previous, viewModel.uiState.value)
        }

    @Test
    fun delivery_saveCancelsStaleReadAndResumeDoesNotReloadDuringSave() =
        runTest(dispatcher) {
            var reads = 0
            val stale = CompletableDeferred<ReceiverDeliveryConditions>()
            val saved = CompletableDeferred<ReceiverDeliveryConditions>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetReceiverDeliveryConditions = { if (++reads == 1) delivery() else stale.await() }
                    onUpdateReceiverDeliveryConditions = { _, _ -> saved.await() }
                }
            val viewModel = deliveryViewModel(repository)
            runCurrent()
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            runCurrent()
            viewModel.onIntent(DeliveryConditionIntent.SelectConditionType(1))
            viewModel.onIntent(DeliveryConditionIntent.Save)
            viewModel.onIntent(DeliveryConditionIntent.Save)
            viewModel.onIntent(DeliveryConditionIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(1, repository.deliveryUpdateCalls.size)
            assertEquals(2, reads)
            saved.complete(delivery(DeliveryConditionType.RECEIVER_REQUEST))
            runCurrent()
            stale.complete(delivery())
            runCurrent()
            assertEquals(
                DeliveryConditionType.RECEIVER_REQUEST,
                viewModel.uiState.value.conditions
                    .single()
                    .conditionType,
            )
        }

    @Test
    fun profile_firstResumeAndPendingLoadDoNotDuplicateAndRefreshKeepsContent() =
        runTest(dispatcher) {
            var calls = 0
            var response = CompletableDeferred<User>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = {
                        calls++
                        response.await()
                    }
                }
            val viewModel = ProfileEditViewModel(repository)
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(1, calls)
            response.complete(user())
            runCurrent()
            val previous = viewModel.uiState.value
            response = CompletableDeferred()
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(2, calls)
            assertEquals(previous, viewModel.uiState.value)
            response.complete(user().copy(name = "새 이름"))
            runCurrent()
            assertEquals("새 이름", (viewModel.uiState.value as ProfileEditUiState.Success).name)
        }

    @Test
    fun profile_failedRefreshPreservesLoadedForm() =
        runTest(dispatcher) {
            val repository = FakeUserRepository.strict().apply { onGetMyProfile = { user() } }
            val viewModel = ProfileEditViewModel(repository)
            runCurrent()
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            val previous = viewModel.uiState.value
            repository.onGetMyProfile = { error("offline") }
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(previous, viewModel.uiState.value)
        }

    @Test
    fun profile_successfulReentryRecoversInitialError() =
        runTest(dispatcher) {
            val repository = FakeUserRepository.strict().apply { onGetMyProfile = { error("offline") } }
            val viewModel = ProfileEditViewModel(repository)
            runCurrent()
            assertEquals(ProfileEditUiState.Error, viewModel.uiState.value)
            repository.onGetMyProfile = { user() }
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            runCurrent()
            assertTrue(viewModel.uiState.value is ProfileEditUiState.Success)
        }

    @Test
    fun profile_saveCancelsStaleReadAndResumeDoesNotReloadDuringSave() =
        runTest(dispatcher) {
            var reads = 0
            val stale = CompletableDeferred<User>()
            val saved = CompletableDeferred<User>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = { if (++reads == 1) user() else stale.await() }
                    onUpdateMyProfile = { _, _, _ -> saved.await() }
                }
            val viewModel = ProfileEditViewModel(repository)
            runCurrent()
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            runCurrent()
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("작성한 이름", "01012345678"))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("중복", "01000000000"))
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(2, reads)
            assertEquals(1, repository.profileUpdateCalls.size)
            saved.completeExceptionally(IllegalStateException("save failed"))
            runCurrent()
            stale.complete(user().copy(name = "stale"))
            runCurrent()
            assertEquals("이름", (viewModel.uiState.value as ProfileEditUiState.Success).name)
            assertFalse((viewModel.uiState.value as ProfileEditUiState.Success).isUpdating)
        }

    @Test
    fun push_firstResumeAndPendingLoadDoNotDuplicateAndRefreshKeepsContent() =
        runTest(dispatcher) {
            var calls = 0
            var response = CompletableDeferred<UserPushSetting>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyPushSettings = {
                        calls++
                        response.await()
                    }
                }
            val viewModel = pushViewModel(repository)
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(1, calls)
            response.complete(UserPushSetting(false, false, false))
            runCurrent()
            val previous = viewModel.uiState.value
            response = CompletableDeferred()
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(2, calls)
            assertEquals(previous, viewModel.uiState.value)
            response.complete(UserPushSetting(true, true, true))
            runCurrent()
            assertTrue(viewModel.uiState.value.isNewsletterOn)
            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertTrue(viewModel.uiState.value.isAfternoteOn)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun push_failedRefreshPreservesLoadedState() =
        runTest(dispatcher) {
            val repository = FakeUserRepository.strict().apply { onGetMyPushSettings = { UserPushSetting(true, true, true) } }
            val viewModel = pushViewModel(repository)
            runCurrent()
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            val previous = viewModel.uiState.value
            repository.onGetMyPushSettings = { error("offline") }
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(previous, viewModel.uiState.value)
        }

    @Test
    fun push_saveCancelsStaleReadAndResumeDoesNotReloadDuringSave() =
        runTest(dispatcher) {
            var reads = 0
            val stale = CompletableDeferred<UserPushSetting>()
            val saved = CompletableDeferred<UserPushSetting>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyPushSettings = { if (++reads == 1) UserPushSetting(false, false, false) else stale.await() }
                    onUpdateMyPushSettings = { _, _, _ -> saved.await() }
                }
            val viewModel = pushViewModel(repository)
            runCurrent()
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            runCurrent()
            viewModel.onIntent(PushNotificationIntent.NewsletterToggle(true))
            viewModel.onIntent(PushNotificationIntent.RefreshOnReturn)
            runCurrent()
            assertEquals(2, reads)
            saved.complete(UserPushSetting(true, false, false))
            runCurrent()
            stale.complete(UserPushSetting(false, false, false))
            runCurrent()
            assertTrue(viewModel.uiState.value.isNewsletterOn)
        }

    private fun deliveryViewModel(repository: FakeUserRepository) =
        DeliveryConditionViewModel(SettingRoute.AfterDeliveryRoute(receiverId = 42L), repository)

    private fun pushViewModel(repository: FakeUserRepository) =
        PushNotificationViewModel(
            ApplicationProvider.getApplicationContext(),
            repository.apply { onGetMyMarketingConsents = { UserMarketingConsent(true, true, false) } },
            NoOpErrorReporter,
        )

    private fun user() = User("이름", "user@example.com", "01012345678", null)

    private fun accounts(google: Boolean = false) = UserConnectedAccount(true, google, false, false, false, null, null, null, null, null)

    private fun delivery(type: DeliveryConditionType = DeliveryConditionType.INACTIVITY) =
        ReceiverDeliveryConditions(
            receiverId = 42L,
            conditions =
                listOf(
                    DeliveryConditionItem(
                        contentType = DeliveryContentType.TIME_LETTER,
                        conditionType = type,
                        inactivityPeriod = InactivityPeriod.SIX_MONTHS.takeIf { type == DeliveryConditionType.INACTIVITY },
                        state = ConditionState.ACTIVE,
                        fulfilled = false,
                        gracePeriodStartedAt = null,
                        fulfilledAt = null,
                    ),
                ),
        )
}
