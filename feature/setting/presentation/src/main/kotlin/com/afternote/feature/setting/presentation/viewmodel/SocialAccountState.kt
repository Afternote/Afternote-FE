package com.afternote.feature.setting.presentation.viewmodel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class SocialAccountState(
    val provider: String,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    val isConnected: Boolean,
    val isLinkable: Boolean = true,
    val email: String? = null,
)
