package com.afternote.feature.setting.domain

import com.afternote.core.model.user.UserConnectedAccount

public interface SettingAccountRepository {
    // 회원 탈퇴
    public suspend fun deleteAccount()

    // 연결된 계정 조회
    public suspend fun getConnectedAccounts(): UserConnectedAccount

    // 소셜 계정 연결
    public suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount

    // 소셜 계정 연결 해제
    public suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount
}
