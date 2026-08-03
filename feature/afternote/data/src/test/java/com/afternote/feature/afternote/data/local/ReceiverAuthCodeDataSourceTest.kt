package com.afternote.feature.afternote.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiverAuthCodeDataSourceTest {
    @Test
    fun `clear - 저장된 수신자 인증 코드를 제거한다`() =
        runBlocking {
            val dataSource = ReceiverAuthCodeDataSource(InMemoryPreferencesDataStore())
            dataSource.saveCode("AUTH-CODE-A")
            assertEquals("AUTH-CODE-A", dataSource.savedCodeFlow.first())

            dataSource.clear()

            assertNull(dataSource.savedCodeFlow.first())
        }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val transformed = transform(state.value)
        state.value = transformed
        return transformed
    }
}
