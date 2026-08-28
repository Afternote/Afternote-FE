package com.afternote.core.domain.repository

import com.afternote.core.model.user.UserPushSetting

/**
 * 로그인한 사용자의 푸시 알림 설정 조회·변경 계약 (#1282).
 */
interface MyPushSettingRepository {
    // 푸시 알림 설정 조회
    suspend fun getMyPushSettings(): UserPushSetting

    // 푸시 알림 설정 수정
    suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting
}
