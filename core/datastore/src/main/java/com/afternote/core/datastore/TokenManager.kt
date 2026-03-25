package com.afternote.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("Token")

private val TOKEN_KEY = stringPreferencesKey("access_token")
private val USER_ID_KEY = longPreferencesKey("user_id")

class TokenManager
    @Inject
    constructor(
        @param:ApplicationContext val context: Context,
    ) {
        suspend fun saveToken(token: String) {
            val dataStore = context.dataStore
            dataStore.edit {
                it[TOKEN_KEY] = token
            }
        }

        suspend fun getToken(): String? {
            val dataStore = context.dataStore
            val dataFlow = dataStore.data
            val prefs = dataFlow.first()
            return prefs[TOKEN_KEY]
        }

        suspend fun getUserId(): Long? {
            val dataStore = context.dataStore
            val dataFlow = dataStore.data
            val prefs = dataFlow.first()
            return prefs[USER_ID_KEY]
        }
    }
