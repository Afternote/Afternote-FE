package com.kuit.afternote.core.presentation

interface Step {
    val value: Int

    fun previous(): Step?
}
