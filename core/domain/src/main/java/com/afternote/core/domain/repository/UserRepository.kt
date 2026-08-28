package com.afternote.core.domain.repository

/**
 * 사용자 도메인 통합 계약 — 전환기 전용 (#1282).
 *
 * 멤버는 전부 책임별 좁은 계약 4종으로 이동했고, 이 인터페이스는 그 합집합이다.
 * 기존 소비자가 좁은 계약으로 이관되는 동안만 남으며, 전 소비자 이관 후 제거한다.
 * 새 코드는 이 인터페이스가 아니라 필요한 좁은 계약만 주입받는다.
 *
 * - [UserReceiverRepository] — 사용자 계정에 등록된 수신자 목록·CRUD·메시지·전달조건
 * - [MyProfileRepository] — 서버 정본 프로필 조회·수정
 * - [MyAccountRepository] — 로그인된 계정의 탈퇴·소셜 연결 계정
 * - [MyPushSettingRepository] — 푸시 알림 설정 조회·변경
 */
interface UserRepository :
    UserReceiverRepository,
    MyProfileRepository,
    MyAccountRepository,
    MyPushSettingRepository
