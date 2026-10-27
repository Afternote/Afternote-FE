package com.afternote.feature.setting.domain.testing

import com.afternote.feature.setting.domain.NotificationAnniversary
import com.afternote.feature.setting.domain.NotificationDelegate
import com.afternote.feature.setting.domain.NotificationPreferenceRepository
import com.afternote.feature.setting.domain.NotificationPreferences

/**
 * 서버 알림 설정을 쓰지 않는 테스트의 Hilt 그래프를 채우는 대역 (#2321). 불리면 그 자체가 회귀라 던진다.
 */
public object StrictNotificationPreferenceRepository : NotificationPreferenceRepository {
    override suspend fun getPreferences(): Result<NotificationPreferences> = unexpected()

    override suspend fun updatePreferences(preferences: NotificationPreferences): Result<NotificationPreferences> = unexpected()

    override suspend fun getDelegate(): Result<NotificationDelegate?> = unexpected()

    override suspend fun updateDelegate(delegate: NotificationDelegate): Result<Unit> = unexpected()

    override suspend fun deleteDelegate(): Result<Unit> = unexpected()

    override suspend fun getAnniversaries(): Result<List<NotificationAnniversary>> = unexpected()

    override suspend fun addAnniversary(
        receiverId: Long,
        month: Int,
        day: Int,
    ): Result<NotificationAnniversary> = unexpected()

    override suspend fun deleteAnniversary(anniversaryId: Long): Result<Unit> = unexpected()

    private fun unexpected(): Nothing = error("이 테스트는 서버 알림 설정을 부르지 않는다")
}
