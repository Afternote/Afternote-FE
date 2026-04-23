package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ConnectedAccountsViewModel.kt
@HiltViewModel
class ConnectedAccountsViewModel
    @Inject
    constructor() : ViewModel() {
        private val _accounts =
            MutableStateFlow(
                listOf(
                    SocialAccountState(R.drawable.ic_naver_logo, R.string.login_with_naver, false),
                    SocialAccountState(
                        R.drawable.ic_google_logo,
                        R.string.login_with_google,
                        true,
                        "example@gmail.com",
                    ),
                    SocialAccountState(
                        R.drawable.ic_kakao_logo,
                        R.string.login_with_kakao,
                        true,
                        "example@kakao.com",
                    ),
                    SocialAccountState(R.drawable.ic_apple_logo, R.string.login_with_apple, false),
                ),
            )
        val accounts: StateFlow<List<SocialAccountState>> = _accounts.asStateFlow()

        fun onToggleAccount(
            iconRes: Int,
            enabled: Boolean,
        ) {
            _accounts.update { list ->
                list.map { if (it.iconRes == iconRes) it.copy(isConnected = enabled) else it }
            }
        }
    }
