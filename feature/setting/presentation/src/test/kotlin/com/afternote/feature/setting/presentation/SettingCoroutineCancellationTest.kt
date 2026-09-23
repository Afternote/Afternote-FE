package com.afternote.feature.setting.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import com.afternote.feature.setting.presentation.account.ConnectedAccountsViewModel
import com.afternote.feature.setting.presentation.home.SettingUiState
import com.afternote.feature.setting.presentation.home.SettingViewModel
import com.afternote.feature.setting.presentation.notification.PushNotificationEvent
import com.afternote.feature.setting.presentation.notification.PushNotificationViewModel
import com.afternote.feature.setting.presentation.profile.ProfileEditEvent
import com.afternote.feature.setting.presentation.profile.ProfileEditUiState
import com.afternote.feature.setting.presentation.profile.ProfileEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * 설정 화면 ViewModel 의 suspend 저장소 호출이 코루틴 취소를 실패로 둔갑시키지 않는지 검증한다 (#1501).
 *
 * 취소는 화면 이탈로 [ViewModel.onCleared] 가 불릴 때 실제로 일어나므로, 여기서는 진짜
 * [ViewModelStore] 에 담았다가 [ViewModelStore.clear] 로 끊는다. 저장소 fake 는 응답하지 않고
 * 매달려 있어, 취소가 대기 중인 호출까지 닿았는지를 [PendingRepositoryCall.isCancelled] 로 본다.
 * 예외를 직접 던지는 검사에 더해, 실제 ViewModel 종료가 진행 중 요청을 취소하는 경로까지 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingCoroutineCancellationTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `연결 계정 조회 중 화면을 떠나면 오류 문구로 덮지 않는다`() =
        runTest(dispatcher) {
            val pending = PendingRepositoryCall()
            val repository =
                FakeSettingAccountRepository().apply {
                    onGetConnectedAccounts = { pending.await() }
                }
            val viewModel = ConnectedAccountsViewModel(repository)
            val store = storeHolding(viewModel)
            runCurrent()
            assertTrue(pending.isStarted)
            val pendingState = viewModel.uiState.value

            store.clear()
            runCurrent()

            assertTrue(pending.isCancelled)
            assertEquals(pendingState, viewModel.uiState.value)
            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `계정 연결 중 화면을 떠나면 연결 실패 문구를 띄우지 않는다`() =
        runTest(dispatcher) {
            val pending = PendingRepositoryCall()
            val repository =
                FakeSettingAccountRepository().apply {
                    onLinkConnectedAccount = { _, _ -> pending.await() }
                }
            val viewModel = ConnectedAccountsViewModel(repository)
            val store = storeHolding(viewModel)
            runCurrent()

            viewModel.link(provider = "google", accessToken = "google-token")
            runCurrent()
            assertTrue(pending.isStarted)
            val pendingState = viewModel.uiState.value

            store.clear()
            runCurrent()

            assertTrue(pending.isCancelled)
            assertEquals(pendingState, viewModel.uiState.value)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `계정 연결 해제 중 화면을 떠나면 해제 실패 문구를 띄우지 않는다`() =
        runTest(dispatcher) {
            val pending = PendingRepositoryCall()
            val repository =
                FakeSettingAccountRepository().apply {
                    onUnlinkConnectedAccount = { pending.await() }
                }
            val viewModel = ConnectedAccountsViewModel(repository)
            val store = storeHolding(viewModel)
            runCurrent()

            viewModel.onToggle(provider = "naver", enabled = false)
            runCurrent()
            assertTrue(pending.isStarted)
            val pendingState = viewModel.uiState.value

            store.clear()
            runCurrent()

            assertTrue(pending.isCancelled)
            assertEquals(pendingState, viewModel.uiState.value)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `프로필 편집 조회 중 화면을 떠나면 Error 상태로 바꾸지 않는다`() =
        runTest(dispatcher) {
            val pending = PendingRepositoryCall()
            val repository =
                FakeMyProfileRepository().apply {
                    onGetMyProfile = { pending.await() }
                }
            val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())
            val store = storeHolding(viewModel)
            runCurrent()
            assertTrue(pending.isStarted)

            store.clear()
            runCurrent()

            assertTrue(pending.isCancelled)
            assertEquals(ProfileEditUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `프로필 수정 중 화면을 떠나면 갱신 표시를 풀거나 실패 이벤트를 보내지 않는다`() =
        runTest(dispatcher) {
            val pending = PendingRepositoryCall()
            val repository =
                FakeMyProfileRepository(profile = PROFILE).apply {
                    onUpdateMyProfile = { _, _, _ -> pending.await() }
                }
            val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())
            val store = storeHolding(viewModel)
            val events = mutableListOf<ProfileEditEvent>()
            backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
            runCurrent()

            viewModel.updateProfile(name = "새 이름", phone = "01011112222")
            runCurrent()
            assertTrue(pending.isStarted)

            store.clear()
            runCurrent()

            assertTrue(pending.isCancelled)
            assertEquals(
                ProfileEditUiState.Success(
                    name = PROFILE.name,
                    phone = PROFILE.phone.orEmpty(),
                    email = PROFILE.email,
                    profileImageUrl = PROFILE.profileImageUrl,
                    isUpdating = true,
                ),
                viewModel.uiState.value,
            )
            assertTrue(events.isEmpty())
        }

    @Test
    fun `프로필 사진 업로드 중 화면을 떠나면 수정 요청도 실패 이벤트도 보내지 않는다`() =
        runTest(dispatcher) {
            val pending = PendingRepositoryCall()
            val profileRepository =
                FakeMyProfileRepository.strict().apply {
                    onGetMyProfile = { PROFILE }
                }
            val uploadRepository = FakePhotoUploadRepository(onUpload = { _, _ -> pending.await() })
            val viewModel = ProfileEditViewModel(profileRepository, uploadRepository)
            val store = storeHolding(viewModel)
            val events = mutableListOf<ProfileEditEvent>()
            backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
            runCurrent()

            viewModel.selectProfileImage(PICKED_PHOTO)
            viewModel.updateProfile(name = "새 이름", phone = "01011112222")
            runCurrent()
            assertTrue(pending.isStarted)

            store.clear()
            runCurrent()

            assertTrue(pending.isCancelled)
            assertTrue(profileRepository.profileUpdateCalls.isEmpty())
            assertTrue(events.isEmpty())
        }

    @Test
    fun `설정 홈 프로필 조회 중 화면을 떠나면 Error 상태로 바꾸지 않는다`() =
        runTest(dispatcher) {
            val pending = PendingRepositoryCall()
            val viewModel =
                SettingViewModel(
                    authRepository = FakeAuthRepository.strict(),
                    myProfileRepository =
                        FakeMyProfileRepository.strict().apply {
                            onGetMyProfile = { pending.await() }
                        },
                    accountRepository = FakeSettingAccountRepository.strict(),
                )
            val store = storeHolding(viewModel)
            runCurrent()
            assertTrue(pending.isStarted)

            store.clear()
            runCurrent()

            assertTrue(pending.isCancelled)
            assertEquals(SettingUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `마케팅 동의 조회 중 화면을 떠나면 상태 진단 어느 것도 남기지 않는다`() =
        runTest(dispatcher) {
            val pending = PendingRepositoryCall()
            val repository =
                FakeSettingNotificationRepository().apply {
                    onGetMyPushSettings = { UserPushSetting(timeLetter = true, mindRecord = false, afterNote = true) }
                    onGetMyMarketingConsents = { pending.await() }
                }
            val reporter = RecordingErrorReporter()
            val viewModel = pushViewModel(repository, reporter)
            val store = storeHolding(viewModel)
            val events = mutableListOf<PushNotificationEvent>()
            backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
            runCurrent()

            // 푸시 설정 조회는 먼저 정상 종료해 안정된 상태를 만든다. 매달린 것은 마케팅 조회뿐이다.
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(pending.isStarted)
            val stableState = viewModel.uiState.value

            store.clear()
            runCurrent()

            assertTrue(pending.isCancelled)
            assertEquals(stableState, viewModel.uiState.value)
            assertTrue(events.isEmpty())
            assertEquals(0, reporter.recordedStages.size)
            assertEquals(emptyList<String>(), pushFailureLogs())
        }

    @Test
    fun `마케팅 동의 조회는 성공하면 상태에 반영하고 평범한 실패는 로그만 남긴다`() =
        runTest(dispatcher) {
            val successReporter = RecordingErrorReporter()
            val successViewModel =
                pushViewModel(
                    FakeSettingNotificationRepository().apply {
                        onGetMyPushSettings = { UserPushSetting(timeLetter = false, mindRecord = false, afterNote = false) }
                        onGetMyMarketingConsents = { UserMarketingConsent(sms = false, email = true, push = true) }
                    },
                    successReporter,
                )
            runCurrent()

            assertFalse(successViewModel.uiState.value.isSmsChecked)
            assertTrue(successViewModel.uiState.value.isEmailChecked)
            assertTrue(successViewModel.uiState.value.isPushChecked)
            assertEquals(emptyList<String>(), pushFailureLogs())

            ShadowLog.clear()
            val failureReporter = RecordingErrorReporter()
            val failureViewModel =
                pushViewModel(
                    FakeSettingNotificationRepository().apply {
                        onGetMyPushSettings = { UserPushSetting(timeLetter = false, mindRecord = false, afterNote = false) }
                        onGetMyMarketingConsents = { error("marketing consents unavailable") }
                    },
                    failureReporter,
                )
            val events = mutableListOf<PushNotificationEvent>()
            backgroundScope.launch(dispatcher) { failureViewModel.events.collect { events += it } }
            runCurrent()

            // 평범한 실패는 진단 로그 한 줄로 끝난다. 화면 값은 기본값 그대로이고 안내도 띄우지 않는다.
            assertEquals(listOf("loadMarketingConsents: failed"), pushFailureLogs())
            assertTrue(failureViewModel.uiState.value.isSmsChecked)
            assertTrue(failureViewModel.uiState.value.isEmailChecked)
            assertFalse(failureViewModel.uiState.value.isPushChecked)
            assertTrue(events.isEmpty())
            assertEquals(0, failureReporter.recordedStages.size)
        }

    @Test
    fun `Exception 이 아닌 Throwable 실패도 평소대로 오류 상태로 매핑한다`() =
        runTest(dispatcher) {
            val viewModel =
                SettingViewModel(
                    authRepository = FakeAuthRepository.strict(),
                    myProfileRepository =
                        FakeMyProfileRepository.strict().apply {
                            onGetMyProfile = { throw NonExceptionFailure() }
                        },
                    accountRepository = FakeSettingAccountRepository.strict(),
                )
            runCurrent()

            assertEquals(SettingUiState.Error("프로필을 불러올 수 없습니다."), viewModel.uiState.value)
        }

    private fun pushViewModel(
        repository: FakeSettingNotificationRepository,
        reporter: ErrorReporter,
    ) = PushNotificationViewModel(
        context = ApplicationProvider.getApplicationContext(),
        notificationRepository = repository,
        errorReporter = reporter,
    )

    /** 푸시 ViewModel 이 남긴 실패 진단 로그의 메시지 목록. 성공 경로의 `Log.d` 는 세지 않는다. */
    private fun pushFailureLogs(): List<String> =
        ShadowLog
            .getLogsForTag(PUSH_TAG)
            .filter { it.type >= Log.ERROR }
            .map { it.msg }

    private fun storeHolding(viewModel: ViewModel): ViewModelStore = ViewModelStore().apply { put(STORE_KEY, viewModel) }

    private class RecordingErrorReporter : ErrorReporter {
        val recordedStages = mutableListOf<String?>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            recordedStages += attributes["stage"]
        }
    }

    /** `Exception` 을 상속하지 않는 실패. `runCatching` 계열이 `Throwable` 까지 잡는다는 계약을 고정한다. */
    private class NonExceptionFailure : Throwable("profile backend down")

    private companion object {
        const val STORE_KEY = "setting-cancellation"
        const val PUSH_TAG = "PushNotificationVM"
        const val PICKED_PHOTO = "content://media/picker/0/profile/1"
        val PROFILE = User(name = "기존 이름", email = "user@afternote.local", phone = "01000000000", profileImageUrl = null)
    }
}

/**
 * 응답하지 않는 저장소 호출. 취소가 이 자리까지 닿았는지를 [isCancelled] 로 드러낸다.
 *
 * 취소 콜백(`invokeOnCancellation`)은 코루틴이 취소될 때만 호출되므로, 대기 중인 suspend 호출이
 * 실제로 끊겼다는 증거가 된다.
 */
private class PendingRepositoryCall {
    @Volatile
    var isStarted = false
        private set

    @Volatile
    var isCancelled = false
        private set

    suspend fun <T> await(): T =
        suspendCancellableCoroutine { continuation ->
            isStarted = true
            continuation.invokeOnCancellation { isCancelled = true }
        }
}
