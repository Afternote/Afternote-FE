package com.afternote.feature.timeletter.domain.model

data class RecordedAudio(
    val uriString: String,
    val fileName: String,
    val mimeType: String,
    val durationMillis: Long,
)
