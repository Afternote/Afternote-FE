package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.datastore.UserProfileDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PassKeyViewModel
    @Inject
    constructor(
        private val dataSource: UserProfileDataSource,
    ) : ViewModel() {
        val isPasskeyRegistered: StateFlow<Boolean?> =
            dataSource
                .isPasskeyRegisteredFlow()
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        fun savePasskeyRegistered() {
            viewModelScope.launch {
                dataSource.savePasskeyRegistered(true)
            }
        }
    }
