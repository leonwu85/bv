package dev.aaa1115910.bv.viewmodel.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.login.QrLoginState
import dev.aaa1115910.biliapi.repositories.LoginRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.BlacklistUtil
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fError
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.timeTask
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import java.util.Timer

@KoinViewModel
class AppQrLoginViewModel(
    private val userRepository: UserRepository,
    private val loginRepository: LoginRepository
) : ViewModel() {
    var state by mutableStateOf(QrLoginState.Ready)
    private val logger = KotlinLogging.logger { }
    var loginUrl by mutableStateOf("")
    private var key = ""
    private var qrLoginApiType = ApiType.App

    private var timer = Timer()

    fun requestQRCode(
        preferApiType: ApiType = ApiType.App,
        webQrSource: String? = null,
        webQrGoUrl: String? = null
    ) {
        state = QrLoginState.Ready
        loginUrl = ""
        key = ""
        qrLoginApiType = preferApiType
        logger.fInfo { "Request login qr code with apiType=$preferApiType" }
        runCatching { timer.cancel() }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                withContext(Dispatchers.Main) { state = QrLoginState.RequestingQRCode }
                val qrLoginData = when (preferApiType) {
                    ApiType.Web -> loginRepository.requestWebQrLogin(
                        source = webQrSource,
                        goUrl = webQrGoUrl
                    )
                    ApiType.App -> loginRepository.requestAppQrLogin()
                }
                withContext(Dispatchers.Main) {
                    loginUrl = qrLoginData.url
                    key = qrLoginData.key
                    qrLoginApiType = preferApiType
                }
                logger.fInfo { "Get login request code url with apiType=$preferApiType" }
                logger.info { qrLoginData.url }
                timer = timeTask(1000, 1000, "check qr login result") {
                    viewModelScope.launch {
                        checkLoginResult()
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    it.message?.toast(BVApp.context)
                    state = QrLoginState.Error
                }
                logger.fError { "Get login request code url failed: ${it.stackTraceToString()}" }
                timer.cancel()
            }
        }
    }

    fun cancelCheckLoginResultTimer() {
        timer.cancel()
    }

    private suspend fun checkLoginResult() {
        val currentQrLoginApiType = qrLoginApiType
        logger.fInfo { "Check for login result with apiType=$currentQrLoginApiType" }
        runCatching {
            val qrLoginResult = when (currentQrLoginApiType) {
                ApiType.Web -> loginRepository.checkWebQrLoginState(key)
                ApiType.App -> loginRepository.checkAppQrLoginState(key)
            }
            when (qrLoginResult.state) {
                QrLoginState.WaitingForScan -> {
                    withContext(Dispatchers.Main) { state = qrLoginResult.state }
                    logger.fInfo { "Waiting to scan" }
                }

                QrLoginState.WaitingForConfirm -> {
                    withContext(Dispatchers.Main) { state = qrLoginResult.state }
                    logger.fInfo { "Waiting to confirm" }
                }

                QrLoginState.Expired -> {
                    withContext(Dispatchers.Main) { state = qrLoginResult.state }
                    logger.fInfo { "QR expired" }
                    timer.cancel()
                }

                QrLoginState.Success -> {
                    logger.fInfo { "Login success" }
                    timer.cancel()
                    runCatching {
                        Prefs.buvid3 = loginRepository.getbuvid3()
                    }.onFailure {
                        logger.warn { "Get buvid3 failed: ${it.stackTraceToString()}" }
                    }

                    val authData = AuthData(
                        uid = qrLoginResult.cookies!!.dedeUserId,
                        uidCkMd5 = qrLoginResult.cookies!!.dedeUserIdCkMd5,
                        sid = qrLoginResult.cookies!!.sid,
                        biliJct = qrLoginResult.cookies!!.biliJct,
                        sessData = qrLoginResult.cookies!!.sessData,
                        tokenExpiredData = qrLoginResult.cookies!!.expiredDate.time,
                        accessToken = qrLoginResult.accessToken.orEmpty(),
                        refreshToken = qrLoginResult.refreshToken.orEmpty()
                    )

                    BlacklistUtil.checkUid(authData.uid)
                    userRepository.validateAuthData(authData)
                    userRepository.addUser(authData)
                    withContext(Dispatchers.Main) { state = QrLoginState.Success }
                }

                else -> {
                    withContext(Dispatchers.Main) { state = qrLoginResult.state }
                    logger.fInfo { "This state should not be here: ${qrLoginResult.state}" }
                }
            }
        }.onFailure {
            if (it is CancellationException) {
                logger.fInfo { "Timer job cancelled" }
                return@onFailure
            }
            withContext(Dispatchers.Main) {
                it.message?.toast(BVApp.context)
                state = QrLoginState.Error
            }
            logger.fError { "Check qr state failed: ${it.stackTraceToString()}" }
        }
    }
}
