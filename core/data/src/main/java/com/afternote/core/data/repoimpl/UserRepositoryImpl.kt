package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.data.mapper.user.toDomain
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.UserApiService
import javax.inject.Inject

/**
 * [UserRepository] 전환기 구현 — 좁은 계약 2종에 위임하고 아직 못 내려간 6멤버만 직접 갖는다 (#1282).
 *
 * 수신자는 [UserReceiverRepository], 서버 정본 프로필은 [MyProfileRepository] 의 구현이 소유하고 여기서는
 * 생성자로 주입받은 그 바인딩에 위임만 한다. 수신자 구현은 `@Singleton` 이라 이 합본을 거치든 좁은 계약을
 * 직접 주입받든 같은 인스턴스를 본다 — 좁은 계약으로 만든 수신자가 합본 구독자의 목록도 갱신한다.
 *
 * 남은 계정·푸시 6멤버는 core 에 좁은 계약을 신설하지 않고 `feature:setting` 으로 곧장 내린다 (#1429).
 */
internal class UserRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
        private val authRepository: AuthRepository,
        private val errorReporter: ErrorReporter,
        receiverRepository: UserReceiverRepository,
        myProfileRepository: MyProfileRepository,
    ) : UserRepository,
        UserReceiverRepository by receiverRepository,
        MyProfileRepository by myProfileRepository {
        /**
         * 탈퇴 성공 후 로컬 세션도 정리한다 (#586) — 서버가 계정을 지워도 토큰이 남으면 재시작 시
         * 죽은 토큰으로 홈이 뜨고 인증 요청이 연달아 401 로 실패한다(2026-07-28 에뮬 실측).
         *
         * 정리를 서버 호출 **뒤**에 두는 건 `AuthRepositoryImpl.logout()` 과 같은 이유 — DELETE 요청도
         * `AuthInterceptor` 를 지나므로 그 시점엔 토큰이 살아 있어야 한다.
         *
         * `clearSession()` 의 실패는 삼킨다. 서버 계정은 이미 지워졌으므로 여기서 예외를 올리면
         * 화면이 "탈퇴 실패" 로 표시돼 사용자가 재시도하고, 그 재시도는 없는 계정에 대해 실패한다.
         * 대신 [ErrorReporter] 에 비식별 단계만 붙여 남긴다 — 조용히 넘기면 로컬 토큰이 남은 채로
         * 이 버그가 재발해도 탐지되지 않는다. 예외 원문 제거는 리포터 공통 정책이 담당한다.
         */
        override suspend fun deleteAccount() {
            userApiService
                .deleteAccount()
                .requireStatus()
            authRepository.clearSession().onFailure {
                errorReporter.recordFailure(
                    throwable = it,
                    attributes = mapOf(KEY_ACCOUNT_STAGE to ACCOUNT_STAGE_DELETE_SESSION_CLEANUP),
                )
            }
        }

        override suspend fun getMyPushSettings(): UserPushSetting =
            userApiService
                .getMyPushSettings()
                .requireData()
                .toDomain()

        override suspend fun updateMyPushSettings(
            timeLetter: Boolean?,
            mindRecord: Boolean?,
            afterNote: Boolean?,
        ): UserPushSetting =
            userApiService
                .updateMyPushSettings(
                    UserUpdatePushSettingRequestDto(
                        timeLetter = timeLetter,
                        mindRecord = mindRecord,
                        afterNote = afterNote,
                    ),
                ).requireData()
                .toDomain()

        override suspend fun getConnectedAccounts(): UserConnectedAccount =
            userApiService
                .getConnectedAccounts()
                .requireData()
                .toDomain()

        override suspend fun linkConnectedAccount(
            provider: String,
            accessToken: String,
        ): UserConnectedAccount =
            userApiService
                .linkConnectedAccount(
                    provider = provider,
                    request = SocialAccountLinkRequestDto(accessToken = accessToken),
                ).requireData()
                .toDomain()

        override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount =
            userApiService
                .unlinkConnectedAccount(provider)
                .requireData()
                .toDomain()
    }

private const val KEY_ACCOUNT_STAGE = "account_stage"
private const val ACCOUNT_STAGE_DELETE_SESSION_CLEANUP = "delete_session_cleanup"
