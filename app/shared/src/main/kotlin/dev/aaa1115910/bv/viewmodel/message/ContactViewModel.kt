package dev.aaa1115910.bv.viewmodel.message

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.user.FollowedUser
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.repository.UserRepository as AccountRepository
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class ContactViewModel(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    val followedUsers = mutableStateListOf<FollowedUser>()
    val fanUsers = mutableStateListOf<FollowedUser>()

    var loadingFollowing by mutableStateOf(false)
        private set
    var loadingFans by mutableStateOf(false)
        private set
    var followingError by mutableStateOf<String?>(null)
        private set
    var fanError by mutableStateOf<String?>(null)
        private set

    val isLogin: Boolean get() = accountRepository.isLogin

    init {
        refreshFollowing()
        refreshFans()
    }

    fun refreshFollowing() {
        if (loadingFollowing) return
        if (!accountRepository.isLogin) {
            followedUsers.clear()
            followingError = "请先登录"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingFollowing = true
                followingError = null
            }
            runCatching {
                userRepository.getFollowedUsers(
                    mid = accountRepository.uid,
                    preferApiType = Prefs.apiType
                )
            }.onSuccess { users ->
                withContext(Dispatchers.Main) {
                    followedUsers.clear()
                    followedUsers.addAll(sortUsers(users))
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    followingError = error.message ?: "加载失败"
                }
            }
            withContext(Dispatchers.Main) {
                loadingFollowing = false
            }
        }
    }

    fun refreshFans() {
        if (loadingFans) return
        if (!accountRepository.isLogin) {
            fanUsers.clear()
            fanError = "请先登录"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingFans = true
                fanError = null
            }
            runCatching {
                userRepository.getFanUsers(
                    mid = accountRepository.uid,
                    preferApiType = Prefs.apiType
                )
            }.onSuccess { users ->
                withContext(Dispatchers.Main) {
                    fanUsers.clear()
                    fanUsers.addAll(sortUsers(users))
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    fanError = error.message ?: "加载失败"
                }
            }
            withContext(Dispatchers.Main) {
                loadingFans = false
            }
        }
    }

    private fun sortUsers(users: List<FollowedUser>): List<FollowedUser> =
        users.sortedWith { left, right ->
            Collator.getInstance(Locale.CHINA).compare(left.name, right.name)
        }
}
