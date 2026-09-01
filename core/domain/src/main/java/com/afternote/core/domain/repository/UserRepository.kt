package com.afternote.core.domain.repository

import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting

/**
 * 사용자 도메인 통합 계약 — 전환기 전용 (#1282).
 *
 * 수신자·프로필 멤버는 책임별 좁은 계약 2종으로 이동했고, 이 인터페이스는 그 합집합에 아직
 * 이동하지 않은 계정·푸시 설정 멤버를 더한 것이다. 기존 소비자가 좁은 계약으로 이관되는 동안만
 * 남으며, 전 소비자 이관 후 제거한다. 새 코드는 이 인터페이스가 아니라 필요한 좁은 계약만 주입받는다.
 *
 * - [UserReceiverRepository] — 사용자 계정에 등록된 수신자 목록·CRUD·메시지·전달조건
 * - [MyProfileRepository] — 서버 정본 프로필 조회·수정
 *
 * 아래 계정·푸시 설정 6멤버는 **core 에 좁은 계약을 신설하지 않는다.** main 소스 소비자가
 * `feature:setting` 뿐이라 core 에 계약을 세웠다가 다시 내리면 같은 멤버를 두 번 옮기게 된다.
 * `feature:setting` 으로 곧장 내리는 몫은 #1429 다.
 */
interface UserRepository :
    UserReceiverRepository,
    MyProfileRepository {
    // 회원 탈퇴
    suspend fun deleteAccount()

    // 푸시 알림 설정 조회
    suspend fun getMyPushSettings(): UserPushSetting

    // 푸시 알림 설정 수정
    suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting

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
