package com.afternote.feature.afternote.presentation.model

interface Step {
    val value: Int

    fun previous(): Step?
}
