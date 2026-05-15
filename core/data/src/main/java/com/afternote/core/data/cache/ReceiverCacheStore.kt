package com.afternote.core.data.cache

import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.setting.ReceiverListItem
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
            userRepository
                .getReceivers()
                .onSuccess { list ->
                    _receiverNameMap.value = list.associate { it.receiverId to it.name }
                    _receiverList.value = list
                    isLoaded = true
                }
        }

        fun getReceiverName(id: Long): String = _receiverNameMap.value[id] ?: ""
    }
