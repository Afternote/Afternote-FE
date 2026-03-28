package com.afternote.feature.timeletter.domain

data class TimeLetter(
    val identity: LetterIdentity,
    val schedule: LetterSchedule,
)
