package com.afternote.core.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [ReloadOnReentryEffect] 는 최초 진입의 `ON_RESUME` 은 건너뛰고, 그 다음 재진입부터만
 * [ReloadOnReentryEffect] 의 콜백을 불러야 한다 — 최초 진입에도 부르면 ViewModel `init` 의
 * 최초 로드와 중복 호출된다 (#703 회귀).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReloadOnReentryEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `최초 진입 ON_RESUME 은 건너뛰고 재진입부터 콜백을 부른다`() {
        val owner = TestLifecycleOwner()
        var reloadCalls = 0

        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                ReloadOnReentryEffect { reloadCalls += 1 }
            }
        }
        owner.moveTo(Lifecycle.Event.ON_CREATE)
        owner.moveTo(Lifecycle.Event.ON_START)
        owner.moveTo(Lifecycle.Event.ON_RESUME)
        composeRule.waitForIdle()
        assertEquals(0, reloadCalls)

        owner.moveTo(Lifecycle.Event.ON_PAUSE)
        owner.moveTo(Lifecycle.Event.ON_RESUME)
        composeRule.waitForIdle()
        assertEquals(1, reloadCalls)

        owner.moveTo(Lifecycle.Event.ON_PAUSE)
        owner.moveTo(Lifecycle.Event.ON_RESUME)
        composeRule.waitForIdle()
        assertEquals(2, reloadCalls)
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry

        fun moveTo(event: Lifecycle.Event) {
            registry.handleLifecycleEvent(event)
        }
    }
}
