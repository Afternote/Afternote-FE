package com.afternote.core.data.repoimpl.appversion

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.appversion.AppVersionRepository
import com.afternote.core.model.appversion.AppVersionCheck
import com.afternote.core.network.dto.AppPlatformDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.AppVersionApiService
import javax.inject.Inject

/**
 * `GET /app/version` 의 유일한 소비 지점 (#1539).
 *
 * 플랫폼은 여기서 고정한다 — 서버 enum 은 확장 가능하지만 이 앱이 물어볼 수 있는 값은
 * ANDROID 하나뿐이고, BE 는 그 밖의 값에 `UNSUPPORTED_APP_PLATFORM` 을 낸다.
 */
internal class AppVersionRepositoryImpl
    @Inject
    constructor(
        private val appVersionApiService: AppVersionApiService,
    ) : AppVersionRepository {
        override suspend fun checkAndroidVersion(versionCode: Int): Result<AppVersionCheck> =
            runCatchingCancellable {
                appVersionApiService
                    .checkVersion(platform = AppPlatformDto.ANDROID, versionCode = versionCode)
                    .requireData()
                    .let { dto ->
                        AppVersionCheck(
                            updateRequired = dto.updateRequired,
                            latestVersionCode = dto.latestVersionCode,
                            storeUrl = dto.storeUrl,
                        )
                    }
            }
    }
