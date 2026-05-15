package dev.aaa1115910.bv.mobile.component.videocard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.repositories.LikeRepository
import dev.aaa1115910.biliapi.repositories.RecommendVideoRepository
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.carddata.VideoCardFeedOption
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun VideoCardMoreMenu(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    toViewRepository: ToViewRepository = koinInject(),
    likeRepository: LikeRepository = koinInject(),
    userRepository: UserRepository = koinInject(),
    recommendVideoRepository: RecommendVideoRepository = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bvid = data.resolvedBvid
    var expanded by remember { mutableStateOf(false) }
    var showDislikeDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var incognitoMode by remember { mutableStateOf(MobilePrefs.incognitoMode) }

    fun runAction(
        successText: String,
        action: suspend () -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            runCatching { action() }
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        successText.toast(context)
                    }
                }
                .onFailure {
                    withContext(Dispatchers.Main) {
                        (it.localizedMessage ?: "操作失败").toast(context)
                    }
                }
        }
    }

    fun requireLogin(): Boolean {
        if (!Prefs.isLogin) {
            "账号未登录".toast(context)
            return false
        }
        return true
    }

    IconButton(
        modifier = modifier.size(32.dp),
        onClick = { expanded = true }
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "更多"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(text = bvid.ifBlank { "未获取到 BV 号" }) },
            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
            enabled = bvid.isNotBlank(),
            onClick = {
                expanded = false
                copyText(context, "BV 号", bvid)
                "已复制 $bvid".toast(context)
            }
        )
        DropdownMenuItem(
            text = { Text(text = "稍后再看") },
            leadingIcon = { Icon(Icons.Rounded.WatchLater, contentDescription = null) },
            onClick = {
                expanded = false
                if (!requireLogin()) return@DropdownMenuItem
                val bvid = data.resolvedBvid.takeIf { it.isNotBlank() }
                val avid = data.avid.takeIf { it > 0L }
                if (bvid == null && avid == null) {
                    "未获取到视频 ID".toast(context)
                } else {
                    runAction("已添加到稍后再看") {
                        toViewRepository.addToView(avid = avid, bvid = bvid)
                    }
                }
            }
        )
        DropdownMenuItem(
            text = { Text(text = "访问：${data.upName}") },
            leadingIcon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
            onClick = {
                expanded = false
                if (data.upId > 0L) {
                    UserSpaceActivity.actionStart(context, data.upId, data.upName)
                } else {
                    "未获取到 UP 主信息".toast(context)
                }
            }
        )
        DropdownMenuItem(
            text = { Text(text = "不感兴趣") },
            leadingIcon = { Icon(Icons.Outlined.ThumbDown, contentDescription = null) },
            onClick = {
                expanded = false
                if (!requireLogin()) return@DropdownMenuItem
                if (data.dislikeReasons.isNotEmpty() || data.feedbacks.isNotEmpty()) {
                    showDislikeDialog = true
                } else {
                    val avid = data.avid.takeIf { it > 0L }
                    if (avid == null) {
                        "当前卡片不支持不感兴趣".toast(context)
                    } else {
                        runAction("已减少此类推荐") {
                            likeRepository.addVideoDislike(avid)
                        }
                    }
                }
            }
        )
        DropdownMenuItem(
            text = { Text(text = "拉黑：${data.upName}") },
            leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) },
            onClick = {
                expanded = false
                if (!requireLogin()) return@DropdownMenuItem
                if (data.upId > 0L) {
                    showBlacklistDialog = true
                } else {
                    "未获取到 UP 主信息".toast(context)
                }
            }
        )
        DropdownMenuItem(
            text = { Text(text = if (incognitoMode) "退出无痕模式" else "进入无痕模式") },
            leadingIcon = {
                Icon(
                    imageVector = if (incognitoMode) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null
                )
            },
            onClick = {
                expanded = false
                incognitoMode = !incognitoMode
                MobilePrefs.incognitoMode = incognitoMode
                (if (incognitoMode) "已进入无痕模式" else "已退出无痕模式").toast(context)
            }
        )
    }

    if (showDislikeDialog) {
        VideoDislikeReasonDialog(
            data = data,
            onDismiss = { showDislikeDialog = false },
            onSelectReason = { option, isFeedback ->
                showDislikeDialog = false
                if (data.feedGoto.isBlank() || data.feedParam.isBlank()) {
                    "当前卡片不支持精细反馈".toast(context)
                    return@VideoDislikeReasonDialog
                }
                runAction(option.toast.ifBlank { "已减少此类推荐" }) {
                    recommendVideoRepository.dislikeRecommendation(
                        goto = data.feedGoto,
                        id = data.feedParam,
                        reasonId = option.id.takeUnless { isFeedback },
                        feedbackId = option.id.takeIf { isFeedback }
                    )
                }
            },
            onCancelFeedback = {
                showDislikeDialog = false
                if (data.feedGoto.isBlank() || data.feedParam.isBlank()) {
                    "当前卡片不支持撤销反馈".toast(context)
                    return@VideoDislikeReasonDialog
                }
                runAction("已撤销") {
                    recommendVideoRepository.cancelDislikeRecommendation(
                        goto = data.feedGoto,
                        id = data.feedParam
                    )
                }
            }
        )
    }

    if (showBlacklistDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            title = { Text(text = "拉黑 UP 主") },
            text = {
                Text(
                    text = "确定拉黑：${data.upName}(${data.upId})？\n\n被拉黑的 UP 主可以在 B 站隐私设置的黑名单管理中解除。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlacklistDialog = false
                        runAction("已拉黑：${data.upName}") {
                            val success = userRepository.blacklistUser(data.upId)
                            if (!success) error("拉黑失败")
                        }
                    }
                ) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlacklistDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Composable
private fun VideoDislikeReasonDialog(
    data: VideoCardData,
    onDismiss: () -> Unit,
    onSelectReason: (VideoCardFeedOption, isFeedback: Boolean) -> Unit,
    onCancelFeedback: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "不感兴趣") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (data.dislikeReasons.isNotEmpty()) {
                    Text(
                        text = "我不想看",
                        style = MaterialTheme.typography.titleSmall
                    )
                    data.dislikeReasons.forEach { option ->
                        TextButton(onClick = { onSelectReason(option, false) }) {
                            Text(text = option.name)
                        }
                    }
                }
                if (data.feedbacks.isNotEmpty()) {
                    Text(
                        text = "反馈",
                        style = MaterialTheme.typography.titleSmall
                    )
                    data.feedbacks.forEach { option ->
                        TextButton(onClick = { onSelectReason(option, true) }) {
                            Text(text = option.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancelFeedback) {
                Text(text = "撤销")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

private fun copyText(
    context: Context,
    label: String,
    text: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun shareText(
    context: Context,
    text: String,
    title: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
