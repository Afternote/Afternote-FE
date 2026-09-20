package com.afternote.feature.setting.domain

import java.time.LocalDate

/** 공지 목록 화면(`:feature:setting:presentation`)이 소비하는 모듈 밖 계약이라 `public` 이다. */
public data class Notice(
    val date: LocalDate,
    val title: String,
    val content: String,
)
