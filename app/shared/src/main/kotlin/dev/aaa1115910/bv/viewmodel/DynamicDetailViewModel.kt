package dev.aaa1115910.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.user.ArticleParagraph
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.repositories.LikeRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DynamicDetailViewModel(
    private val userRepository: UserRepository,
    private val likeRepository: LikeRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var dynamicId by mutableStateOf("")
    var dynamicItem by mutableStateOf<DynamicItem?>(null)

    // 专栏内容相关状态
    var articleParagraphs by mutableStateOf<List<ArticleParagraph>>(emptyList())
        private set
    var isLoadingArticle by mutableStateOf(false)
        private set
    var articleLoadError by mutableStateOf<String?>(null)
        private set

    // 点赞相关状态
    var isLiked by mutableStateOf(false)
        private set
    var likeCount by mutableStateOf(0)
        private set
    var isLiking by mutableStateOf(false)
        private set

    suspend fun loadDynamic() {
        logger.fInfo { "Loading dynamic detail: $dynamicId" }
        runCatching {
            dynamicItem = userRepository.getDynamicDetail(
                dynamicId = dynamicId,
                preferApiType = Prefs.apiType
            )
            // 加载完成后初始化点赞状态（从动态数据中直接读取）
            dynamicItem?.let { item ->
                likeCount = item.footer?.like ?: 0
                isLiked = item.footer?.isLiked ?: false
            }
        }.onFailure {
            logger.fException(it) { "Failed to load dynamic" }
            withContext(Dispatchers.Main) {
                "Failed to load dynamic: ${it.message}".toast(BVApp.context)
            }
        }
    }

    /**
     * 加载专栏完整内容
     * 在 loadDynamic 之后调用，仅对 Article 类型动态有效
     */
    suspend fun loadArticleContent() {
        if (dynamicItem?.type != DynamicType.Article) return
        if (isLoadingArticle) return

        isLoadingArticle = true
        articleLoadError = null

        logger.fInfo { "Loading article content for: $dynamicId" }
        runCatching {
            val opusDetail = userRepository.getOpusDetailResult(
                opusId = dynamicId,
                preferApiType = Prefs.apiType
            )
            articleParagraphs = opusDetail.paragraphs
            updateCommentTarget(opusDetail.commentId, opusDetail.commentType)
            logger.fInfo { "Loaded ${articleParagraphs.size} paragraphs" }
        }.onFailure {
            logger.fException(it) { "Failed to load article content" }
            articleLoadError = it.message
            // 加载失败不显示 toast，回退到摘要显示
        }

        isLoadingArticle = false
    }

    private fun updateCommentTarget(commentId: Long, commentType: Long) {
        if (commentId <= 0L || commentType <= 0L) return
        val item = dynamicItem ?: return
        if (item.commentId == commentId && item.commentType == commentType) return
        dynamicItem = item.copy(
            commentId = commentId,
            commentType = commentType
        )
    }

    fun toggleLike() {
        if (isLiking) return
        val itemId = dynamicItem?.id ?: return

        viewModelScope.launch {
            isLiking = true
            runCatching {
                if (isLiked) {
                    likeRepository.delDynamicLike(
                        dynamicId = itemId,
                        preferApiType = Prefs.apiType
                    )
                    isLiked = false
                    likeCount = maxOf(0, likeCount - 1)
                } else {
                    likeRepository.addDynamicLike(
                        dynamicId = itemId,
                        preferApiType = Prefs.apiType
                    )
                    isLiked = true
                    likeCount += 1
                }
            }.onFailure {
                logger.fException(it) { "Failed to toggle like" }
                withContext(Dispatchers.Main) {
                    "操作失败: ${it.message}".toast(BVApp.context)
                }
            }
            isLiking = false
        }
    }
}
