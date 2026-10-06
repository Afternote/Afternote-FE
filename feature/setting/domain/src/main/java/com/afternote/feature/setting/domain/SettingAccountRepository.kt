package com.afternote.feature.setting.domain

import com.afternote.core.model.user.UserConnectedAccount

/**
 * 설정의 계정 계약 (#1429).
 *
 * 탈퇴와 소셜 계정 연결은 설정 밖에 소비자가 없어 `core:domain` 의 사용자 합본이 아니라 이 모듈이
 * 소유한다. 탈퇴는 서버 삭제가 성공한 뒤 로컬 세션 정리까지를 한 호출로 끝낸다 — 호출처가 토큰
 * 정리를 따로 신경 쓰지 않는다.
 */
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
