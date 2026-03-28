package com.afternote.feature.timeletter.domain

class TimeLetters(private val values: List<TimeLetter>) {
    fun isEmpty(): Boolean = values.isEmpty()

    fun toList(): List<TimeLetter> = values
}
