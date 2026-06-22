package dev.aaa1115910.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.http.entity.user.MyInfoData
import dev.aaa1115910.biliapi.http.entity.user.UserNavStatData
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private var shouldUpdateInfo = true
    private val logger = KotlinLogging.logger { }
    val isLogin get() = userRepository.isLogin
    val username get() = userRepository.username
    val face get() = userRepository.avatar

    var responseData: MyInfoData? by mutableStateOf(null)
    var statData: UserNavStatData? by mutableStateOf(null)

    init {
        if (userRepository.isLogin) {
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.reloadAvatar()
            }
        }
    }

    fun updateUserInfo(forceUpdate: Boolean = false) {
        if (!forceUpdate) {
            if (!shouldUpdateInfo || !userRepository.isLogin) return
        } else {
            if (!userRepository.isLogin) return
        }
        logger.fInfo { "Update user info" }
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.reloadAvatar()

            runCatching {
                val data = BiliHttpApi.getUserSelfInfo(sessData = Prefs.sessData).getResponseData()
                withContext(Dispatchers.Main) { responseData = data }
                logger.fInfo { "Update user info success" }
                shouldUpdateInfo = false
                userRepository.username = data.name
                userRepository.avatar = data.face
                userRepository.findUserByUid(Prefs.uid)?.let { user ->
                    user.username = data.name
                    user.avatar = data.face
                    userRepository.updateUser(user)
                }
            }.onFailure {
                when (it) {
                    is AuthFailureException -> {
                        withContext(Dispatchers.Main) {
                            BVApp.context.getString(R.string.exception_auth_failure)
                                .toast(BVApp.context)
                        }
                        logger.fInfo { "User auth failure" }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            "获取用户信息失败：${it.message}".toast(BVApp.context)
                        }
                    }
                }
            }
            runCatching {
                BiliHttpApi.getUserNavStat(
                    buvid3 = Prefs.buvid3,
                    sessData = Prefs.sessData
                ).getResponseData()
            }.onSuccess { data ->
                withContext(Dispatchers.Main) {
                    statData = data
                }
            }.onFailure {
                logger.warn(it) { "Update user nav stat failed" }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.logout()
        }
    }

    fun clearUserInfo() {
        userRepository.username = ""
        userRepository.avatar = ""
        responseData = null
        statData = null
    }
}
