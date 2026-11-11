package dev.aaa1115910.bv.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters

/** Fixed upstreams for the SDK running on the phone's LAN origin. Never forward TV account cookies. */
internal object GaiaCompanionApi {
    private const val API = "https://api.bilibili.com"
    private val client = HttpClient(OkHttp)

    suspend fun config() = client.get("$API/x/web-frontend/risk/config").bodyAsText()
    suspend fun register(form: Parameters) = submit("register", form)
    suspend fun validate(form: Parameters) = submit("validate", form)

    private suspend fun submit(action: String, form: Parameters): String =
        client.submitForm("$API/x/gaia-vgate/v2/$action", form) {
            header("Referer", "https://www.bilibili.com/")
            WebDeviceCookieSync.verificationCookies()?.let { header("Cookie", it.toCookieHeader()) }
        }.bodyAsText()
}
