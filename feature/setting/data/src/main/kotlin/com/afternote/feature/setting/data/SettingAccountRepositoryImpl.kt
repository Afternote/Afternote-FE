package com.afternote.feature.setting.data

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserUpdateMarketingConsentRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.UserApiService
import com.afternote.feature.setting.domain.SettingAccountRepository
import javax.inject.Inject

internal class SettingAccountRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
        private val authRepository: AuthRepository,
        private val errorReporter: ErrorReporter,
    ) : SettingAccountRepository {
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
