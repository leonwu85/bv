package dev.aaa1115910.biliapi.http.util

import java.net.URLEncoder
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDateTime
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val loginSecureRandom = SecureRandom()
private const val LOGIN_RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

fun generateLoginSessionId(buvid: String, timestampMillis: Long): String =
    md5(buvid + timestampMillis.toString())

fun generateLoginDeviceId(now: LocalDateTime = LocalDateTime.now()): String {
    val bytes = buildList {
        repeat(16) { add(loginSecureRandom.nextInt(256)) }
        add(dec2bcd(now.year / 100))
        add(dec2bcd(now.year % 100))
        add(dec2bcd(now.monthValue))
        add(dec2bcd(now.dayOfMonth))
        add(dec2bcd(now.hour))
        add(dec2bcd(now.minute))
        add(dec2bcd(now.second))
        repeat(8) { add(loginSecureRandom.nextInt(256)) }
    }
    val check = bytes.sum() and 0xff
    return bytes.joinToString("") { "%02x".format(it) } + "%02x".format(check)
}

@OptIn(ExperimentalEncodingApi::class)
fun encryptLoginDt(publicKeyPem: String, value: String = generateLoginRandomString()): String {
    val publicKeyContent = publicKeyPem
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\s".toRegex(), "")
    val keySpec = X509EncodedKeySpec(Base64.decode(publicKeyContent))
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    val encrypted = cipher.doFinal(value.toByteArray())
    return encodeLoginComponent(Base64.encode(encrypted))
}

fun encodeLoginComponent(value: String): String = URLEncoder.encode(value, "UTF-8")

private fun generateLoginRandomString(length: Int = 16): String =
    buildString {
        repeat(length) {
            append(LOGIN_RANDOM_CHARS[loginSecureRandom.nextInt(LOGIN_RANDOM_CHARS.length)])
        }
    }

private fun dec2bcd(dec: Int): Int = ((dec / 10) shl 4) or (dec % 10)
