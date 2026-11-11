package dev.aaa1115910.bv.network

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/** The Web Crypto AES-CBC operation unavailable to the SDK on an HTTP LAN page. */
internal object GaiaCompanionCrypto {
    // Captcha proofs are small. Bound both the JSON and decoded input before encryption.
    const val MAX_REQUEST_BYTES = 90_000L
    private const val MAX_DATA_BYTES = 65_536

    fun encrypt(body: String): String {
        require(body.length <= MAX_REQUEST_BYTES)
        val payload = Json.parseToJsonElement(body).jsonObject
        fun decode(name: String): ByteArray {
            val value = payload[name] as? JsonPrimitive
            require(value?.isString == true) { "Missing $name" }
            return Base64.decode(value.content)
        }
        val key = decode("key")
        val iv = decode("iv")
        val data = decode("data")
        require(key.size in listOf(16, 24, 32))
        require(iv.size == 16)
        require(data.size <= MAX_DATA_BYTES)
        // JCE PKCS5Padding for AES is the PKCS#7 padding required by Web Crypto AES-CBC.
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return buildJsonObject {
            put("data", JsonPrimitive(Base64.encode(cipher.doFinal(data))))
        }.toString()
    }
}
