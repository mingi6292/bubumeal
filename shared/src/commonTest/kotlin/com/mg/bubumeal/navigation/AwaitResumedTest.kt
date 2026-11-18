package com.mg.bubumeal.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AwaitResumedTest {
    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    /** 로그인 창(다른 Activity)이 앞에 떠 있어 STARTED인 동안 결과가 와도, 돌아와 RESUMED가 되면 이동한다. */
    @Test
    fun awaitResumed_waitsWhileStarted_thenCompletes() = runTest(UnconfinedTestDispatcher()) {
        val owner = TestOwner().apply { registry.currentState = Lifecycle.State.STARTED }
        var done = false
        launch { owner.lifecycle.awaitResumed(); done = true }
        assertFalse(done)
        owner.registry.currentState = Lifecycle.State.RESUMED
        assertTrue(done)
    }

    @Test
    fun awaitResumed_alreadyResumed_returnsImmediately() = runTest(UnconfinedTestDispatcher()) {
        val owner = TestOwner().apply { registry.currentState = Lifecycle.State.RESUMED }
        var done = false
        launch { owner.lifecycle.awaitResumed(); done = true }
        assertTrue(done)
    }
}
