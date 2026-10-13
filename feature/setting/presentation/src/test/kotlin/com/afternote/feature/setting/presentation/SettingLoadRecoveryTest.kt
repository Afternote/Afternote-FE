package com.afternote.feature.setting.presentation

import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import com.afternote.feature.setting.presentation.account.ConnectedAccountsIntent
import com.afternote.feature.setting.presentation.account.ConnectedAccountsViewModel
import com.afternote.feature.setting.presentation.home.SettingIntent
import com.afternote.feature.setting.presentation.home.SettingProfileState
import com.afternote.feature.setting.presentation.home.SettingViewModel
import com.afternote.feature.setting.presentation.home.WithdrawUiState
import com.afternote.feature.setting.presentation.notification.PushNotificationIntent
import com.afternote.feature.setting.presentation.notification.PushNotificationViewModel
import com.afternote.feature.setting.presentation.notification.PushSetting
import com.afternote.feature.setting.presentation.profile.ProfileEditIntent
import com.afternote.feature.setting.presentation.profile.ProfileEditUiState
import com.afternote.feature.setting.presentation.profile.ProfileEditViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingLoadRecoveryTest {
    private val dispatcher = StandardTestDispatcher()
    private val profile = User("테스트", "test@example.com", "01012345678", null)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun `home retry recovers profile without resetting account operation`() =
        runTest(dispatcher) {
            var reads = 0
            val gate = CompletableDeferred<User>()
            val repository =
                FakeMyProfileRepository.strict().apply {
                    onGetMyProfile = { if (++reads == 1) throw IOException() else gate.await() }
                }
            val vm = SettingViewModel(FakeAuthRepository.strict(), repository, FakeSettingAccountRepository())
            runCurrent()
            assertEquals(SettingProfileState.Error, vm.uiState.value.profile)
            vm.onIntent(SettingIntent.DeleteAccount)
            runCurrent()
            vm.onIntent(SettingIntent.Refresh)
            vm.onIntent(SettingIntent.Refresh)
            runCurrent()
            assertEquals(2, reads)
            assertEquals(WithdrawUiState.Success, vm.uiState.value.withdraw)
            gate.complete(profile)
            runCurrent()
            assertEquals(SettingProfileState.Success(profile.name, profile.email), vm.uiState.value.profile)
            assertEquals(WithdrawUiState.Success, vm.uiState.value.withdraw)
        }

    @Test fun `profile retry is single flight and failed load cannot submit a patch`() =
        runTest(dispatcher) {
            var reads = 0
            val gate = CompletableDeferred<User>()
            val repository =
                FakeMyProfileRepository.strict().apply {
                    onGetMyProfile = { if (++reads == 1) throw IOException() else gate.await() }
                }
            val vm = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())
            runCurrent()
            assertEquals(ProfileEditUiState.Error, vm.uiState.value)
            vm.onIntent(ProfileEditIntent.UpdateProfile("new", "01012345678"))
            assertTrue(repository.profileUpdateCalls.isEmpty())
            vm.onIntent(ProfileEditIntent.RetryLoad)
            vm.onIntent(ProfileEditIntent.RetryLoad)
            assertEquals(ProfileEditUiState.Loading, vm.uiState.value)
            runCurrent()
            assertEquals(2, reads)
            gate.complete(profile)
            runCurrent()
            assertEquals(profile.phone, (vm.uiState.value as ProfileEditUiState.Success).phone)
            vm.onIntent(ProfileEditIntent.RetryLoad)
            runCurrent()
            assertEquals(2, reads)
        }

    @Test fun `account load failure blocks writes and retry restores rows`() =
        runTest(dispatcher) {
            var fail = true
            val repository =
                FakeSettingAccountRepository().apply {
                    onGetConnectedAccounts = { if (fail) throw IOException() else connectedAccounts }
                }
            val vm = ConnectedAccountsViewModel(repository)
            runCurrent()
            assertNotNull(vm.uiState.value.errorMessage)
            vm.onIntent(ConnectedAccountsIntent.Toggle("kakao", false))
            vm.onIntent(ConnectedAccountsIntent.Link("kakao", "unused"))
            runCurrent()
            assertTrue(repository.connectedUnlinkCalls.isEmpty())
            assertTrue(repository.connectedLinkCalls.isEmpty())
            fail = false
            vm.onIntent(ConnectedAccountsIntent.RetryLoad)
            vm.onIntent(ConnectedAccountsIntent.RetryLoad)
            runCurrent()
            assertEquals(2, repository.getConnectedAccountsCalls)
            assertNull(vm.uiState.value.errorMessage)
            assertEquals(4, vm.uiState.value.accounts.size)
        }

    @Test fun `account write failure preserves rows and consumes feedback independently of load state`() =
        runTest(dispatcher) {
            val repository = FakeSettingAccountRepository().apply { onUnlinkConnectedAccount = { throw IOException() } }
            val vm = ConnectedAccountsViewModel(repository)
            runCurrent()
            val rows = vm.uiState.value.accounts
            vm.onIntent(ConnectedAccountsIntent.Toggle("kakao", false))
            vm.onIntent(ConnectedAccountsIntent.Toggle("kakao", false))
            runCurrent()
            assertEquals(listOf("kakao"), repository.connectedUnlinkCalls)
            assertEquals(rows, vm.uiState.value.accounts)
            assertNull(vm.uiState.value.errorMessage)
            val error = requireNotNull(vm.uiState.value.pendingError)
            vm.onIntent(ConnectedAccountsIntent.ConsumeError("another message"))
            assertEquals(error, vm.uiState.value.pendingError)
            vm.onIntent(ConnectedAccountsIntent.ConsumeError(error))
            assertNull(vm.uiState.value.pendingError)
        }

    @Test fun `push load error cannot save default values and retry reads real settings`() =
        runTest(dispatcher) {
            var reads = 0
            var writes = 0
            var fail = true
            val repository =
                FakeSettingNotificationRepository().apply {
                    onGetMyPushSettings = {
                        reads++
                        if (fail) throw IOException() else UserPushSetting(timeLetter = true, mindRecord = true, afterNote = false)
                    }
                    onUpdateMyPushSettings = { _, _, _ ->
                        writes++
                        UserPushSetting(false, false, false)
                    }
                }
            val vm = PushNotificationViewModel(ApplicationProvider.getApplicationContext(), repository, NoOpErrorReporter)
            assertTrue(vm.uiState.value.isLoading)
            vm.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, true))
            runCurrent()
            assertNotNull(vm.uiState.value.errorMessage)
            vm.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, true))
            runCurrent()
            assertEquals(0, writes)
            fail = false
            vm.onIntent(PushNotificationIntent.RetryLoad)
            vm.onIntent(PushNotificationIntent.RetryLoad)
            runCurrent()
            assertEquals(2, reads)
            assertNull(vm.uiState.value.errorMessage)
            assertTrue(vm.uiState.value.isNewsletterOn)
            assertTrue(vm.uiState.value.isMindRecordOn)
        }
}
