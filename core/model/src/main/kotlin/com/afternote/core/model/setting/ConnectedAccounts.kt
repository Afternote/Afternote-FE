package com.afternote.core.model.setting

data class ConnectedAccounts(
    val local: Boolean,
    val google: Boolean,
    val naver: Boolean,
    val kakao: Boolean,
    val apple: Boolean,
    val localEmail: String?,
    val googleEmail: String?,
    val naverEmail: String?,
    val kakaoEmail: String?,
    val appleEmail: String?,
)
