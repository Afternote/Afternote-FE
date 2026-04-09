package com.afternote.feature.afternote.data.local

/**
 * Persists the receiver-facing auth code (e.g. SharedPreferences, DataStore).
 * Read/write/clear are all defined here so the cache can act as SSOT.
 *
 * Consumed by [com.afternote.feature.afternote.data.repositoryimpl.receiver.ReceiverRepositoryImpl].
 *
 * Hilt: [SharedPreferencesReceiverAuthCodeLocalDataSource] is `@Inject` + `@Singleton`;
 * bound to this interface in [com.afternote.feature.afternote.data.di.AfternoteReceiverAuthLocalModule].
 */
interface ReceiverAuthCodeLocalDataSource {
    fun getSavedCode(): String?

    fun saveCode(code: String)

    fun clearCode()
}
