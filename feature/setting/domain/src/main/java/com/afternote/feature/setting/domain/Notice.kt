package com.afternote.feature.setting.domain

import java.time.LocalDate

public data class Notice(
    public val date: LocalDate,
    public val title: String,
    public val content: String,
)
