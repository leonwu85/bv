package dev.aaa1115910.bv.mobile.component.home.dynamic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ForwardToInbox
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.mobile.component.videocard.shareText
import dev.aaa1115910.bv.mobile.util.saveImageToGallery
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private data class DynamicLaterTarget(
    val avid: Long?,
    val bvid: String?
)

private data class DynamicReportReason(
    val type: Int,
    val label: String
)

private val dynamicReportReasons = listOf(
    DynamicReportReason(4, "垃圾广告"),
    DynamicReportReason(8, "引战"),
    DynamicReportReason(1, "色情"),
    DynamicReportReason(5, "人身攻击"),
    DynamicReportReason(3, "违法信息"),
    DynamicReportReason(9, "涉政谣言"),
    DynamicReportReason(10, "涉社会事件谣言"),
    DynamicReportReason(12, "虚假不实信息"),
    DynamicReportReason(13, "违法信息外链"),
    DynamicReportReason(0, "其他")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicMoreMenu(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem,
    onTempBlockAuthor: ((DynamicItem.DynamicAuthorModule) -> Unit)? = null,
    toViewRepository: ToViewRepository = koinInject(),
    userRepository: UserRepository = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    fun requireLogin(): Boolean {
        if (!Prefs.isLogin) {
            "账号未登录".toast(context)
            return false
        }
        return true
    }

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

    IconButton(
        modifier = modifier.size(32.dp),
        onClick = { showSheet = true }
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "更多"
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                dynamicItem.findLaterTarget()?.let { target ->
                    DynamicSheetItem(
                        icon = Icons.Rounded.WatchLater,
                        text = "稍后再看",
                        onClick = {
                            showSheet = false
                            if (!requireLogin()) return@DynamicSheetItem
                            runAction("已添加到稍后再看") {
                                toViewRepository.addToView(
                                    avid = target.avid,
                                    bvid = target.bvid
                                )
                            }
                        }
                    )
                }
                DynamicSheetItem(
                    icon = Icons.Rounded.SaveAlt,
                    text = "保存动态",
                    onClick = {
                        showSheet = false
                        val imageUrl = dynamicItem.primaryImageUrl()
                        if (imageUrl.isNullOrBlank()) {
                            "当前动态没有可保存的图片".toast(context)
                        } else {
                            runAction("动态图片已保存到相册") {
                                saveImageToGallery(context, imageUrl)
                            }
                        }
                    }
                )
                DynamicSheetItem(
                    icon = Icons.Rounded.Share,
                    text = "分享动态",
                    onClick = {
                        showSheet = false
                        dynamicItem.shareTextOrToast(context, "分享动态")
                    }
                )
                DynamicSheetItem(
                    icon = Icons.AutoMirrored.Rounded.ForwardToInbox,
                    text = "分享至消息",
                    onClick = {
                        showSheet = false
                        dynamicItem.shareTextOrToast(context, "分享至消息")
                    }
                )
                if (onTempBlockAuthor != null) {
                    DynamicSheetItem(
                        icon = Icons.Rounded.VisibilityOff,
                        text = "临时屏蔽：${dynamicItem.author.author}",
                        onClick = {
                            showSheet = false
                            onTempBlockAuthor(dynamicItem.author)
                        }
                    )
                }
                DynamicSheetItem(
                    icon = Icons.Rounded.Report,
                    text = "举报",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        showSheet = false
                        if (!requireLogin()) return@DynamicSheetItem
                        if (dynamicItem.id.isNullOrBlank() || dynamicItem.author.mid <= 0L) {
                            "当前动态不支持举报".toast(context)
                        } else {
                            showReportDialog = true
                        }
                    }
                )
            }
        }
    }

    if (showReportDialog) {
        DynamicReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reasonType, reasonDesc ->
                showReportDialog = false
                runAction("举报成功") {
                    userRepository.reportDynamic(
                        accusedUid = dynamicItem.author.mid,
                        dynamicId = dynamicItem.id!!,
                        reasonType = reasonType,
                        reasonDesc = reasonDesc
                    )
                }
            }
        )
    }
}

@Composable
private fun DynamicSheetItem(
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun DynamicReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (reasonType: Int, reasonDesc: String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf<DynamicReportReason?>(null) }
    var reasonDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "举报") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = "请选择举报理由：",
                    style = MaterialTheme.typography.bodyMedium
                )
                dynamicReportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(text = reason.label)
                    }
                }
                if (selectedReason?.type == 0) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        value = reasonDesc,
                        onValueChange = { reasonDesc = it },
                        label = { Text(text = "补充说明") },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reason = selectedReason ?: return@TextButton
                    if (reason.type == 0 && reasonDesc.isBlank()) {
                        return@TextButton
                    }
                    onSubmit(reason.type, reasonDesc.takeIf { reason.type == 0 })
                }
            ) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

private fun DynamicItem.findLaterTarget(): DynamicLaterTarget? {
    video?.let { video ->
        val avid = video.aid.takeIf { it > 0L }
        val bvid = video.bvid?.takeIf { it.isNotBlank() }
        if (avid != null || bvid != null) return DynamicLaterTarget(avid, bvid)
    }
    return orig?.findLaterTarget()
}

private fun DynamicItem.primaryImageUrl(): String? {
    return draw?.images?.firstOrNull()?.url
        ?: video?.cover
        ?: pgc?.cover
        ?: article?.covers?.firstOrNull()
        ?: liveRcmd?.cover
        ?: orig?.primaryImageUrl()
}

private fun DynamicItem.shareTextOrToast(
    context: android.content.Context,
    title: String
) {
    val link = id
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://t.bilibili.com/$it" }
        ?: jumpUrl
    if (link.isNullOrBlank()) {
        "当前动态没有可分享链接".toast(context)
    } else {
        shareText(context, link, title)
    }
}
