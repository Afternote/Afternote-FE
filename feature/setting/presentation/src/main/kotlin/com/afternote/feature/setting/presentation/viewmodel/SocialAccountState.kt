package com.afternote.feature.setting.presentation.viewmodel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class SocialAccountState(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    val isConnected: Boolean,
    val email: String? = null,
)
