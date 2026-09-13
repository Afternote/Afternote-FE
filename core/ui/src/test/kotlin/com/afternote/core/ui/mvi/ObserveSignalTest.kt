package com.afternote.core.ui.mvi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 일회성 신호 소비 관용구 (#1800).
 *
 * 화면이 직접 `LaunchedEffect` 를 쓰지 않고 [ObserveSignal] 하나로 소비하도록,
 * 「한 번만 부른다 · 소비가 Intent 로 돌아온다 · 같은 값이 다시 와도 다시 부른다」 를 잠근다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ObserveSignalTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `신호를 한 번 부르고 소비 Intent 를 되쏜다`() {
        val observed = mutableListOf<String>()
        val intents = mutableListOf<SignalIntent>()
        var signal by mutableStateOf<String?>(null)

        composeRule.setContent {
            ObserveSignal(
                signal = signal,
                consumed = SignalIntent.ConsumeError,
                onIntent = { intent ->
                    intents += intent
                    // ViewModel 이 하는 일 — ConsumeXxx 가 신호를 null 로 되돌린다.
                    signal = null
                },
                onSignal = observed::add,
            )
        }

        composeRule.runOnIdle { signal = "보낼 수 없습니다" }
        composeRule.waitForIdle()

        assertEquals(listOf("보낼 수 없습니다"), observed)
        assertEquals(listOf<SignalIntent>(SignalIntent.ConsumeError), intents)
    }

    @Test
    fun `같은 신호가 연속 두 번 와도 두 번 소비된다`() {
        val observed = mutableListOf<String>()
        var signal by mutableStateOf<String?>(null)

        composeRule.setContent {
            ObserveSignal(
                signal = signal,
                consumed = SignalIntent.ConsumeError,
                onIntent = { signal = null },
                onSignal = observed::add,
            )
        }

        repeat(2) {
            composeRule.runOnIdle { signal = "보낼 수 없습니다" }
            composeRule.waitForIdle()
        }

        assertEquals(listOf("보낼 수 없습니다", "보낼 수 없습니다"), observed)
    }

    @Test
    fun `신호가 null 이면 아무것도 부르지 않는다`() {
        val observed = mutableListOf<String>()
        val intents = mutableListOf<SignalIntent>()

        composeRule.setContent {
            ObserveSignal(
                signal = null,
                consumed = SignalIntent.ConsumeError,
                onIntent = intents::add,
                onSignal = observed::add,
            )
        }

        composeRule.waitForIdle()

        assertEquals(emptyList<String>(), observed)
        assertEquals(emptyList<SignalIntent>(), intents)
    }
}

private sealed interface SignalIntent : MviIntent {
    data object ConsumeError : SignalIntent
}
