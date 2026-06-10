package dev.aaa1115910.bv.viewmodel.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.repositories.LoginRepository
import dev.aaa1115910.biliapi.repositories.SendSmsState
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.BlacklistUtil
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fDebug
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.KoinViewModel
import java.net.URI
import java.net.URLDecoder

@KoinViewModel
class SmsLoginViewModel(
    private val userRepository: UserRepository,
    private val loginRepository: LoginRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger { }
    var sendSmsState by mutableStateOf(SendSmsState.Ready)

    private var phone: Long = 0
    private var recaptchaToken: String? = null
    var geetestChallenge: String? = null
    var geetestValidate: String? = null
    var geetestSeccode: String? = null
    private var geetestGt: String? = null
    private val buvid = Prefs.buvid
    private var captchaKey: String? = null

    suspend fun sendSms(
        phone: Long,
        onCaptcha: (challenge: String, gt: String) -> Unit
    ) {
        this.phone = phone
        logger.info { "Send sms to $phone" }
        runCatching {
            val sendSmsResult = loginRepository.requestSms(
                phone = phone,
                buvid = buvid,
                recaptchaToken = recaptchaToken,
                geetestChallenge = geetestChallenge,
                geetestValidate = geetestValidate,
                geetestSeccode = geetestSeccode
            )
            when (sendSmsResult.state) {
                SendSmsState.Ready -> {
                    logger.info { "this state should be here: $sendSmsState" }
                    withContext(Dispatchers.Main) { sendSmsState = sendSmsResult.state }
                }

                SendSmsState.Error -> {
                    logger.warn { "Send sms failed: ${sendSmsResult.message}" }
                    withContext(Dispatchers.Main) {
                        sendSmsState = sendSmsResult.state
                        "发送短信失败：${sendSmsResult.message}".toast(BVApp.context)
                    }
                    clearCaptchaData()
                }

                SendSmsState.Success -> {
                    logger.info { "Send sms success" }
                    captchaKey = sendSmsResult.captchaKey
                    withContext(Dispatchers.Main) {
                        sendSmsState = sendSmsResult.state
                        "验证码已发送".toast(BVApp.context)
                    }
                }

                SendSmsState.RecaptchaRequire -> {
                    logger.info { "Require manual recaptcha" }
                    logger.info { "recaptcha url: ${sendSmsResult.recaptchaUrl}" }

                    if (!loadCaptchaData(sendSmsResult.recaptchaUrl)) {
                        logger.warn { "Load captcha data failed" }
                        withContext(Dispatchers.Main) {
                            sendSmsState = SendSmsState.Error
                            "获取验证码失败，请尝试其它登录方式".toast(BVApp.context)
                        }
                        clearCaptchaData()
                        return
                    }

                    logger.info { "recaptchaToken: $recaptchaToken" }
                    logger.info { "geetestGt: $geetestGt" }
                    logger.info { "geetestChallenge: $geetestChallenge" }
                    onCaptcha(geetestChallenge!!, geetestGt!!)
                    withContext(Dispatchers.Main) { sendSmsState = sendSmsResult.state }
                }
            }
        }.onFailure {
            logger.warn { "Send sms failed: ${it.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                "发送短信失败：${it.message}".toast(BVApp.context)
                clearCaptchaData()
            }
        }
    }

    private suspend fun loadCaptchaData(recaptchaUrl: String?): Boolean {
        if (!recaptchaUrl.isNullOrBlank()) {
            runCatching {
                parseQuery(URI(recaptchaUrl).rawQuery.orEmpty()).forEach { (key, value) ->
                    when (key) {
                        "recaptcha_token" -> recaptchaToken = value
                        "gee_gt" -> geetestGt = value
                        "gee_challenge" -> geetestChallenge = value
                    }
                }
            }.onFailure {
                logger.warn { "Parse recaptcha url failed: ${it.message}" }
            }
        }

        if (isCaptchaDataReady()) return true

        return runCatching {
            val captcha = loginRepository.preCapture()
            recaptchaToken = captcha.token
            geetestGt = captcha.gt
            geetestChallenge = captcha.challenge
            isCaptchaDataReady()
        }.getOrElse {
            logger.warn { "Pre capture failed: ${it.stackTraceToString()}" }
            false
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&")
            .mapNotNull { parameter ->
                val index = parameter.indexOf("=")
                if (index <= 0) return@mapNotNull null
                val key = URLDecoder.decode(parameter.substring(0, index), "UTF-8")
                val value = URLDecoder.decode(parameter.substring(index + 1), "UTF-8")
                key to value
            }
            .toMap()
    }

    private fun isCaptchaDataReady(): Boolean =
        recaptchaToken?.isNotBlank() == true &&
                geetestGt?.isNotBlank() == true &&
                geetestChallenge?.isNotBlank() == true

    suspend fun loginWithSms(code: Int, onSuccess: () -> Unit) {
        logger.info { "Login with sms code: $code" }
        runCatching {
            val loginResult = loginRepository.loginWithSms(
                phone = phone,
                buvid = buvid,
                code = code,
                captchaKey = captchaKey!!
            )
            if (loginResult.status == 0) {
                val authData = AuthData(
                    uid = loginResult.dedeUserId,
                    uidCkMd5 = loginResult.dedeUserIdCkMd5,
                    sid = loginResult.sid,
                    sessData = loginResult.sessData,
                    biliJct = loginResult.biliJct,
                    tokenExpiredData = loginResult.expiredDate.time,
                    accessToken = loginResult.accessToken,
                    refreshToken = loginResult.refreshToken
                )
                BlacklistUtil.checkUid(Prefs.uid)
                userRepository.addUser(authData)

                withContext(Dispatchers.Main) {
                    "登录成功".toast(BVApp.context)
                }
                logger.info { "Login with sms success" }
                logger.fDebug { "$loginResult" }
                onSuccess()
            } else {
                logger.warn { "Login with sms return a unknown response: [status=${loginResult.status}, message=${loginResult.message}]" }
                withContext(Dispatchers.Main) {
                    "未知情况：${loginResult.message}".toast(BVApp.context)
                }
            }
        }.onFailure {
            logger.warn { "Login with sms failed: ${it.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                "短信登录失败：${it.message}".toast(BVApp.context)
            }
        }
    }

    fun clearCaptchaData() {
        logger.info { "Clear captcha data" }
        recaptchaToken = null
        geetestChallenge = null
        geetestValidate = null
        geetestSeccode = null
        sendSmsState = SendSmsState.Ready
    }
}

@Serializable
data class GeetestResult(
    @SerialName("geetest_challenge")
    val geetestChallenge: String,
    @SerialName("geetest_validate")
    val geetestValidate: String,
    @SerialName("geetest_seccode")
    val geetestSeccode: String
)
