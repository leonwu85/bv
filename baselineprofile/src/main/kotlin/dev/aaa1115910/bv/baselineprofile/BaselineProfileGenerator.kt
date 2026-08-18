package dev.aaa1115910.bv.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    /**
     * Covers the cold first home navigation interactions observed in Perfetto:
     * - launch -> first frame -> Recommend/Popular/Recommend/Popular;
     * - Popular/Recommend -> drawer Home/UGC -> UGC content -> drawer UGC/Home -> Home content.
     *
     * The CUJ intentionally stays out of the Startup Profile. It should be AOT compiled by
     * the Baseline Profile, but code reached after the first frame should not be forced into
     * startup DEX layout.
    */
    @Test
    fun homeFirstNavigation() = baselineProfileRule.collect(
        packageName = BuildConfig.TARGET_PACKAGE,
        includeInStartupProfile = false,
    ) {
        pressHome()
        startActivityAndWait()

        assertTrue(
            "Target app did not become visible after launch",
            device.wait(
                Until.hasObject(By.pkg(BuildConfig.TARGET_PACKAGE)),
                APP_VISIBLE_TIMEOUT_MS,
            ),
        )

        // startActivityAndWait() returns after the first frame. Press immediately so this
        // captures the cold path before the fixed-delay adjacent-page preload can hide it.
        assertTrue("DPAD_RIGHT was not injected", device.pressDPadRight())
        SystemClock.sleep(FIRST_SWITCH_SETTLE_MS)

        assertTrue("DPAD_LEFT was not injected", device.pressDPadLeft())
        SystemClock.sleep(WARM_SWITCH_SETTLE_MS)

        assertTrue("Second DPAD_RIGHT was not injected", device.pressDPadRight())
        SystemClock.sleep(FIRST_SWITCH_SETTLE_MS)

        // Return to the left-most top tab, enter the drawer, switch Home -> UGC and then
        // UGC -> Home. This covers DrawerContent's delayed commit, KeepAlivePages preparation,
        // and focus restoration in both directions.
        assertTrue("DPAD_LEFT did not return to Recommend", device.pressDPadLeft())
        SystemClock.sleep(WARM_SWITCH_SETTLE_MS)

        assertTrue("DPAD_LEFT did not enter the drawer", device.pressDPadLeft())
        SystemClock.sleep(DRAWER_FOCUS_SETTLE_MS)

        assertTrue("DPAD_DOWN did not select UGC", device.pressDPadDown())
        SystemClock.sleep(DRAWER_PAGE_SWITCH_SETTLE_MS)

        assertTrue("DPAD_RIGHT did not enter UGC content", device.pressDPadRight())
        SystemClock.sleep(CONTENT_FOCUS_SETTLE_MS)

        assertTrue("DPAD_LEFT did not return to the UGC drawer item", device.pressDPadLeft())
        SystemClock.sleep(DRAWER_FOCUS_SETTLE_MS)

        assertTrue("DPAD_UP did not select Home", device.pressDPadUp())
        SystemClock.sleep(DRAWER_PAGE_SWITCH_SETTLE_MS)

        assertTrue("DPAD_RIGHT did not return to Home content", device.pressDPadRight())
        SystemClock.sleep(CONTENT_FOCUS_SETTLE_MS)
    }

    private companion object {
        const val APP_VISIBLE_TIMEOUT_MS = 10_000L
        const val FIRST_SWITCH_SETTLE_MS = 1_200L
        const val WARM_SWITCH_SETTLE_MS = 750L
        const val DRAWER_FOCUS_SETTLE_MS = 250L
        const val DRAWER_PAGE_SWITCH_SETTLE_MS = 1_200L
        const val CONTENT_FOCUS_SETTLE_MS = 750L
    }
}
