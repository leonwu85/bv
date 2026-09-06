package dev.aaa1115910.bv.player.impl.vlc

/** Native builds that must fall back to Media3 before starting playback. */
internal object VlcNativeCompatibility {
    fun unsupportedReason(version: String?, abi: String): String? {
        // Crashlytics fc460b700c19d7aabfb2b3bf0ac2eddc: this exact ARMv7 build
        // (Build ID 1e16829967f32fb99f259c532ed5f3c126bd49f4) crashes in
        // gnutls_record_send2 -> nettle_gcm_encrypt -> nettle_memxor. A SIGSEGV
        // cannot be caught by the player factory. Keep this build out of playback
        // until an upstream native replacement has been verified on ARMv7.
        return if (version == "4.0.0-eap29" && abi == "armeabi-v7a") {
            "VLC $version on $abi has a native TLS crash; use Media3 or VLC 3"
        } else {
            null
        }
    }
}
