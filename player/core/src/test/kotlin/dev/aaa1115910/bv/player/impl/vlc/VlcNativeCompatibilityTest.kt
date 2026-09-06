package dev.aaa1115910.bv.player.impl.vlc

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VlcNativeCompatibilityTest {
    @Test
    fun affectedVlc4Arm32BuildIsRejectedBeforeNativePlayback() {
        assertNotNull(VlcNativeCompatibility.unsupportedReason("4.0.0-eap29", "armeabi-v7a"))
    }

    @Test
    fun stableAndLegacyVlc3RemainAvailableOnArm32() {
        for (version in listOf("3.7.5", "3.6.5", "3.x", null)) {
            assertNull(VlcNativeCompatibility.unsupportedReason(version, "armeabi-v7a"))
        }
    }

    @Test
    fun otherVlc4ArchitecturesAreNotBlocked() {
        for (abi in listOf("arm64-v8a", "x86", "x86_64")) {
            assertNull(VlcNativeCompatibility.unsupportedReason("4.0.0-eap29", abi))
        }
    }
}
