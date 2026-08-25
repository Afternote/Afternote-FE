package com.afternote.afternote_fe.test

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserProfileRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.timeletter.domain.model.RecordedAudio
import com.afternote.feature.timeletter.domain.repository.VoiceRecorderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object FakeVoiceRecorderRepository : VoiceRecorderRepository {
    override suspend fun start(): Result<Unit> = error("start should not be called")

    override suspend fun stop(): Result<RecordedAudio> = error("stop should not be called")

    override suspend fun discard() = Unit

    override fun retainRecordedFile() = Unit

    override suspend fun deleteRecordedFile(uriString: String) = Unit

    override fun release() = Unit
}

class FakeAuthRepository(
    loggedIn: Boolean = false,
) : AuthRepository {
    private val loggedInState = MutableStateFlow(loggedIn)
    override val isLoggedIn: Flow<Boolean> = loggedInState

    val emailLoginResults = ArrayDeque<Result<Session.DefaultSession>>()
    val attemptedEmailLogins = mutableListOf<Pair<String, String>>()
    var saveSessionCalls = 0
        private set
    var logoutCalls = 0
        private set

    override suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession> {
        attemptedEmailLogins += email to password
        return emailLoginResults.removeFirstOrNull()
            ?: Result.success(Session.DefaultSession("access", "refresh"))
    }

    override suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> {
        saveSessionCalls += 1
        loggedInState.value = true
        return Result.success(Unit)
    }

    override suspend fun clearSession(): Result<Unit> {
        loggedInState.value = false
        return Result.success(Unit)
    }

    override suspend fun logout(): Result<Unit> {
        logoutCalls += 1
        loggedInState.value = false
        return Result.success(Unit)
    }

    override suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun getAccessToken(): Result<String?> = Result.success(null)

    override suspend fun getRefreshToken(): Result<String?> = Result.success(null)

    override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> = unexpected("kakaoLogin")

    override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> = unexpected("googleLogin")

    override suspend fun rotateToken(): Result<TokenBundle> = unexpected("rotateToken")
}

class FakeUserRepository(
    var profile: User = User(name = "테스트 사용자", email = "test@afternote.local", phone = null, profileImageUrl = null),
    receivers: List<Receiver> = listOf(testReceiver()),
    var pushSetting: UserPushSetting = UserPushSetting(true, true, true),
) : UserRepository {
    private val receiverState = MutableStateFlow(receivers)
    override val receiverListFlow: Flow<List<Receiver>> = receiverState

    var getProfileCalls = 0
        private set
    var getReceiversCalls = 0
        private set
    var logActivityCalls = 0
        private set
    var deleteAccountCalls = 0
        private set
    val pushSettingUpdates = mutableListOf<Triple<Boolean?, Boolean?, Boolean?>>()
    val pushSettingUpdateResults = ArrayDeque<Result<UserPushSetting>>()

    override suspend fun getReceivers(): List<Receiver> {
        getReceiversCalls += 1
        return receiverState.value
    }

    override suspend fun getMyProfile(): User {
        getProfileCalls += 1
        return profile
    }

    override suspend fun logActivity() {
        logActivityCalls += 1
    }

    override suspend fun deleteAccount() {
        deleteAccountCalls += 1
    }

    override suspend fun createReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String?,
        message: String?,
    ): ReceiverCreated {
        val id = (receiverState.value.maxOfOrNull(Receiver::receiverId) ?: 0L) + 1L
        receiverState.value = receiverState.value + Receiver(id, name, relation, "fake-auth-$id")
        return ReceiverCreated(id, "fake-auth-$id")
    }

    override suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail = unexpected("getReceiverDetail")

    override suspend fun updateReceiver(
        receiverId: Long,
        name: String,
        phone: String,
        relation: String,
        email: String,
    ): Receiver = unexpected("updateReceiver")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        message: String,
    ) = unexpected<Unit>("updateReceiverMessage")

    override suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User {
        profile =
            profile.copy(
                name = name ?: profile.name,
                phone = phone ?: profile.phone,
                profileImageUrl = profileImageUrl ?: profile.profileImageUrl,
            )
        return profile
    }

    override suspend fun getMyPushSettings(): UserPushSetting = pushSetting

    override suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting {
        pushSettingUpdates += Triple(timeLetter, mindRecord, afterNote)
        val next =
            pushSettingUpdateResults.removeFirstOrNull()
                ?: Result.success(
                    UserPushSetting(
                        timeLetter = timeLetter ?: pushSetting.timeLetter,
                        mindRecord = mindRecord ?: pushSetting.mindRecord,
                        afterNote = afterNote ?: pushSetting.afterNote,
                    ),
                )
        return next.getOrThrow().also { pushSetting = it }
    }

    override suspend fun getConnectedAccounts(): UserConnectedAccount =
        UserConnectedAccount(
            local = true,
            google = false,
            naver = false,
            kakao = false,
            apple = false,
            localEmail = profile.email,
            googleEmail = null,
            naverEmail = null,
            kakaoEmail = null,
            appleEmail = null,
        )

    override suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount = unexpected("linkConnectedAccount")

    override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount = unexpected("unlinkConnectedAccount")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions =
        unexpected("getReceiverDeliveryConditions")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        conditions: List<DeliveryConditionItem>,
    ): ReceiverDeliveryConditions = unexpected("updateReceiverDeliveryConditions")
}

class FakeUserProfileRepository : UserProfileRepository {
    private val passkeyRegistered = MutableStateFlow(false)
    var cachedUserName: String? = null

    override fun isPasskeyRegisteredFlow(): Flow<Boolean> = passkeyRegistered

    override suspend fun savePasskeyRegistered(registered: Boolean) {
        passkeyRegistered.value = registered
    }

    override suspend fun getCachedUserName(): String? = cachedUserName

    override suspend fun saveUserName(name: String) {
        cachedUserName = name
    }
}

class FakeDiaryRepository(
    var listResult: Result<DiaryList> = Result.success(DiaryList(emptyList(), 0, null)),
) : DiaryRepository {
    val createdPayloads = mutableListOf<DiaryCreatePayload>()
    val updatedPayloads = mutableListOf<Pair<Long, DiaryUpdatePayload>>()
    val createResults = ArrayDeque<Result<Unit>>()

    override suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean?,
    ): Result<DiaryList> = listResult

    override suspend fun create(payload: DiaryCreatePayload): Result<Unit> {
        createdPayloads += payload
        return createResults.removeFirstOrNull() ?: Result.success(Unit)
    }

    override suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit> {
        updatedPayloads += id to payload
        return Result.success(Unit)
    }

    override suspend fun delete(id: Long): Result<Unit> = Result.success(Unit)
}

class FakeDailyQuestionRepository(
    var todayResult: Result<TodayDailyQuestion> =
        Result.success(TodayDailyQuestion(1L, 1, "오늘의 질문", false)),
) : DailyQuestionRepository {
    val createdPayloads = mutableListOf<DailyQuestionCreatePayload>()
    val createResults = ArrayDeque<Result<Long>>()

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = Result.success(emptyList())

    override suspend fun getToday(): Result<TodayDailyQuestion> = todayResult

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> {
        createdPayloads += payload
        return createResults.removeFirstOrNull() ?: Result.success(CREATED_DAILY_QUESTION_ID)
    }

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> = Result.success(id)

    override suspend fun delete(id: Long): Result<Unit> = Result.success(Unit)

    private companion object {
        /** 서버가 돌려주는 `userDailyQuestionId` 자리 (#573). 값 자체에 의미는 없다. */
        const val CREATED_DAILY_QUESTION_ID = 1L
    }
}

class FakeErrorReporter : ErrorReporter {
    val failures = mutableListOf<Pair<Throwable, Map<String, String>>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        failures += throwable to attributes
    }
}

fun testReceiver(
    id: Long = 7L,
    name: String = "김수신",
): Receiver = Receiver(receiverId = id, name = name, relation = "가족", authCode = "fake-auth-$id")

private fun <T> unexpected(method: String): T = error("$method 는 이 테스트 시나리오에서 호출되면 안 됩니다.")
