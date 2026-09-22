package com.motivation.pantrypal.util

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

/**
 * android.util.Log isn't available on the plain JVM unit-test classpath
 * (it throws "not mocked" by default). Every production class under test
 * logs liberally per the Part 2 logging requirement, so tests call this
 * once in `@Before` to stub it out harmlessly.
 */
fun mockAndroidLog() {
    mockkStatic(Log::class)
    every { Log.i(any(), any()) } returns 0
    every { Log.d(any(), any()) } returns 0
    every { Log.w(any(), any<String>()) } returns 0
    every { Log.e(any(), any()) } returns 0
    every { Log.e(any(), any(), any()) } returns 0
}
