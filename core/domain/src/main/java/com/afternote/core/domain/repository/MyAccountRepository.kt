package com.afternote.core.domain.repository

import com.afternote.core.model.user.UserConnectedAccount

/**
 * 로그인된 계정의 수명(탈퇴)·소셜 연결 계정 관리 계약 (#1282).
 *
 * [com.afternote.core.domain.repository.account.AccountRepository] 는 회원가입·이메일 인증·
 * 아이디 찾기·비밀번호 변경 등 계정 생성·복구 계약이라 별개 책임이다 — 여기는 로그인 세션이
 * 있는 계정 본인의 관리다.
 */
interface MyAccountRepository {
    // 회원 탈퇴
    suspend fun deleteAccount()

    // 연결된 계정 조회
    suspend fun getConnectedAccounts(): UserConnectedAccount

    // 소셜 계정 연결
    suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount

    // 소셜 계정 연결 해제
    suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount
}
