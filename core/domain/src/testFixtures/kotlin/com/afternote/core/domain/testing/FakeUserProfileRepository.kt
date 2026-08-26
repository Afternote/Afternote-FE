package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** [UserProfileRepository] fake 정본. 패스키 상태와 사용자 이름을 메모리에 보관한다. */
class FakeUserProfileRepository(
    passkeyRegistered: Boolean = false,
    @Volatile var cachedUserName: String? = null,
    var onIsPasskeyRegisteredFlow: (() -> Flow<Boolean>)? = null,
    var onSavePasskeyRegistered: (suspend (Boolean) -> Unit)? = null,
    var onGetCachedUserName: (suspend () -> String?)? = null,
    var onSaveUserName: (suspend (String) -> Unit)? = null,
) : UserProfileRepository {
    val passkeyRegisteredState = MutableStateFlow(passkeyRegistered)
    val savedPasskeyValues = CopyOnWriteArrayList<Boolean>()
    val savedUserNames = CopyOnWriteArrayList<String>()

    private val passkeyFlowCounter = AtomicInteger()
    private val cachedUserNameCounter = AtomicInteger()

    val isPasskeyRegisteredFlowCalls: Int get() = passkeyFlowCounter.get()
    val getCachedUserNameCalls: Int get() = cachedUserNameCounter.get()

    override fun isPasskeyRegisteredFlow(): Flow<Boolean> {
        passkeyFlowCounter.incrementAndGet()
        return onIsPasskeyRegisteredFlow?.invoke() ?: passkeyRegisteredState
    }

    override suspend fun savePasskeyRegistered(registered: Boolean) {
        savedPasskeyValues += registered
        onSavePasskeyRegistered?.let {
            it(registered)
            return
        }
        passkeyRegisteredState.value = registered
    }

    override suspend fun getCachedUserName(): String? {
        cachedUserNameCounter.incrementAndGet()
        onGetCachedUserName?.let { return it() }
        return cachedUserName
    }

    override suspend fun saveUserName(name: String) {
        savedUserNames += name
        onSaveUserName?.let {
            it(name)
            return
        }
        cachedUserName = name
    }

    companion object {
        fun strict(): FakeUserProfileRepository =
            FakeUserProfileRepository(
                onIsPasskeyRegisteredFlow = { unexpectedCall("UserProfileRepository.isPasskeyRegisteredFlow") },
                onSavePasskeyRegistered = { unexpectedCall("UserProfileRepository.savePasskeyRegistered") },
                onGetCachedUserName = { unexpectedCall("UserProfileRepository.getCachedUserName") },
                onSaveUserName = { unexpectedCall("UserProfileRepository.saveUserName") },
            )
    }
}
