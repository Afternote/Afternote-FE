package com.afternote.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 초대 토큰 보관소의 save/read/clear 와 프로세스 재시작 생존 (#944). */
class ReceiverInvitationDataSourceTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun dataSource(scope: CoroutineScope) =
        ReceiverInvitationDataSource(
            PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "ReceiverInvitation.preferences_pb") },
        )

    private fun shutdown(scope: CoroutineScope) =
        runBlocking {
            scope.coroutineContext.job.cancel()
            scope.coroutineContext.job.join()
        }

    @Test
    fun `저장한 토큰은 재시작 뒤에도 읽히고 clear 로 사라진다`() {
        val first = newScope()
        runBlocking {
            val source = dataSource(first)
            assertNull(source.pendingTokenFlow.first())
            source.savePendingToken("token-1")
            assertEquals("token-1", source.pendingTokenFlow.first())
        }
        shutdown(first)

        val second = newScope()
        runBlocking {
            val source = dataSource(second)
            assertEquals("token-1", source.pendingTokenFlow.first())
            source.clearPendingToken()
            assertNull(source.pendingTokenFlow.first())
        }
        shutdown(second)
    }

    @Test
    fun `새 토큰은 이전 토큰을 덮는다`() {
        val scope = newScope()
        runBlocking {
            val source = dataSource(scope)
            source.savePendingToken("token-1")
            source.savePendingToken("token-2")
            assertEquals("token-2", source.pendingTokenFlow.first())
        }
        shutdown(scope)
    }
}
