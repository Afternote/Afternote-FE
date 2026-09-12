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
import com.afternote.feature.setting.domain.SettingNotificationRepository
import javax.inject.Inject

internal class SettingNotificationRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
    ) : SettingNotificationRepository {
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
            mapPushSettingFailure {
                userApiService
                    .updateMyPushSettings(
                        UserUpdatePushSettingRequestDto(
                            timeLetter = timeLetter,
                            mindRecord = mindRecord,
                            afterNote = afterNote,
                        ),
                    ).requireData()
                    .toDomain()
            }

        override suspend fun getMyMarketingConsents(): UserMarketingConsent =
            userApiService
                .getMyMarketingConsents()
                .requireData()
                .toDomain()

        override suspend fun updateMyMarketingConsents(
            sms: Boolean?,
            email: Boolean?,
            push: Boolean?,
        ): UserMarketingConsent =
            userApiService
                .updateMyMarketingConsents(
                    UserUpdateMarketingConsentRequestDto(
                        sms = sms,
                        email = email,
                        push = push,
                    ),
                ).requireData()
                .toDomain()
    }
