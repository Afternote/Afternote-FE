package com.afternote.core.ui

interface Step {
    val value: Int

    fun previous(): Step?
}
