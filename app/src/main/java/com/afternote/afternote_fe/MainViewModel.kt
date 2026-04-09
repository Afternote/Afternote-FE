package com.afternote.afternote_fe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.datastore.TokenManager
import com.afternote.core.ui.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val tokenManager: TokenManager,
    ) : ViewModel() {
        private val _isLoading = MutableStateFlow(true)
        val isLoading = _isLoading.asStateFlow()

        private val _startRoute = MutableStateFlow<Route>(Route.Onboarding)
        val startRoute = _startRoute.asStateFlow()

        init {
            viewModelScope.launch {
                val isLoggedIn = tokenManager.isLoggedInFlow.first()
                _startRoute.value = if (isLoggedIn) Route.Home else Route.Onboarding
                _isLoading.value = false
            }
        }
    }
