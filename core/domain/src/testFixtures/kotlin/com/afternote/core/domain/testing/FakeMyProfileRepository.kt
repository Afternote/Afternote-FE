package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.model.user.User
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [MyProfileRepository] fake 정본 (#1282, #1030).
 *
 * 서버 정본 프로필만 메모리에 담는다. 로컬 캐시(사용자 이름·패스키 등록 여부) fake 는
 * [FakeUserProfileCacheRepository] 로 책임이 다르다.
 *
 * 호출 기록 타입 [ProfileUpdateCall] 은 이 fake 가 소유한다 (#1429) — 이유는
 * [FakeUserReceiverRepository] 와 같다.
 */
class FakeMyProfileRepository(
    @Volatile var profile: User = DEFAULT_USER,
    var onGetMyProfile: (suspend () -> User)? = null,
    var onUpdateMyProfile: (suspend (String?, String?, String?) -> User)? = null,
) : MyProfileRepository {
    private val getProfileCounter = AtomicInteger()

    val profileUpdateCalls = CopyOnWriteArrayList<ProfileUpdateCall>()

    val getProfileCalls: Int get() = getProfileCounter.get()
    val profileCalls: Int get() = getProfileCounter.get()

    override suspend fun getMyProfile(): User {
        getProfileCounter.incrementAndGet()
        onGetMyProfile?.let { return it() }
        return profile
    }

    override suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User {
        profileUpdateCalls += ProfileUpdateCall(name, phone, profileImageUrl)
        onUpdateMyProfile?.let { return it(name, phone, profileImageUrl) }
        profile =
            profile.copy(
                name = name ?: profile.name,
                phone = phone ?: profile.phone,
                profileImageUrl = profileImageUrl ?: profile.profileImageUrl,
            )
        return profile
    }

    data class ProfileUpdateCall(
        val name: String?,
        val phone: String?,
        val profileImageUrl: String?,
    )

    companion object {
        internal val DEFAULT_USER = User("테스트 사용자", "test@afternote.local", null, null)

        fun strict(): FakeMyProfileRepository =
            FakeMyProfileRepository(
                onGetMyProfile = { unexpectedCall("MyProfileRepository.getMyProfile") },
                onUpdateMyProfile = { _, _, _ -> unexpectedCall("MyProfileRepository.updateMyProfile") },
            )
    }
}
