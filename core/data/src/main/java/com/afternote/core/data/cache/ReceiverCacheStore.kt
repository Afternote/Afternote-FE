package com.afternote.core.data.cache

import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.Receiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiverCacheStore
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) {
        private val _receiverNameMap = MutableStateFlow<Map<Long, String>>(emptyMap())
        val receiverNameMap: StateFlow<Map<Long, String>> = _receiverNameMap.asStateFlow()

        private val _receiverList = MutableStateFlow<List<ReceiverListItem>>(emptyList())
        val receiverList: StateFlow<List<ReceiverListItem>> = _receiverList.asStateFlow()

        private var isLoaded = false

        suspend fun ensureLoaded() {
            if (isLoaded) return
            runCatching { userRepository.getReceivers() }
                .onSuccess { list ->
                    _receiverNameMap.value = list.associate { it.receiverId to it.name }
                    _receiverList.value = list.map { it.toReceiverListItem() }
                    isLoaded = true
                }
        }

        suspend fun reload() {
            isLoaded = false
            ensureLoaded()
        }

        private fun Receiver.toReceiverListItem() = ReceiverListItem(receiverId = receiverId, name = name, relation = relation)

        fun getReceiverName(id: Long): String = _receiverNameMap.value[id] ?: ""
    }
