package dev.aaa1115910.bv.network

sealed interface CaptchaRequest {
    data class Geetest(val gt: String, val challenge: String) : CaptchaRequest
    data class Gaia(val voucher: String) : CaptchaRequest
}

sealed interface CaptchaAnswer {
    data class Geetest(val challenge: String, val validate: String, val seccode: String) : CaptchaAnswer
    data class Gaia(val vtoken: String) : CaptchaAnswer
}
