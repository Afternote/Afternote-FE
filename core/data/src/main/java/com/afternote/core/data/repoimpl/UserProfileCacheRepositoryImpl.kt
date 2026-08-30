package com.afternote.core.data.repoimpl

import com.afternote.core.datastore.UserProfileDataSource
import com.afternote.core.domain.repository.UserProfileCacheRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class UserProfileCacheRepositoryImpl
    @Inject
    constructor(
        private val dataSource: UserProfileDataSource,
    ) : UserProfileCacheRepository {
        override fun isPasskeyRegisteredFlow(): Flow<Boolean> = dataSource.isPasskeyRegisteredFlow()

        override suspend fun savePasskeyRegistered(registered: Boolean) {
            dataSource.savePasskeyRegistered(registered)
        }

        override suspend fun getCachedUserName(): String? = dataSource.getCachedUserName()

        override suspend fun saveUserName(name: String) {
            dataSource.saveUserName(name)
        }
    }
