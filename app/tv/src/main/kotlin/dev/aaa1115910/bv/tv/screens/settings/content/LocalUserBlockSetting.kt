package dev.aaa1115910.bv.tv.screens.settings.content

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.PrefKeys
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.component.settings.SettingNumberListItem
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.aaa1115910.bv.tv.component.TvToolbarAction
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.LocalUserBlockViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LocalUserBlockSetting(viewModel: LocalUserBlockViewModel = koinViewModel()) {
    val ids by viewModel.blocked.collectAsState()
    val scope = rememberCoroutineScope()
    val dsm = BVApp.dataStoreManager
    val pure by dsm.getPreferenceFlow(PrefKeys.commentPureMentionRequest).collectAsState(false)
    val limit by dsm.getPreferenceFlow(PrefKeys.commentMentionLimitRequest).collectAsState(0)
    val copy by dsm.getPreferenceFlow(PrefKeys.commentCopyIntroRequest).collectAsState(false)
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val inputFocus = remember { FocusRequester() }
    val addFocus = remember { FocusRequester() }
    fun focusActions() {
        keyboard?.hide()
        addFocus.requestFocus()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("评论过滤") }
        item { SettingSwitchListItem(title = "过滤纯 @ 评论", supportText = "仅隐藏只包含用户提及的评论", checked = pure,
            onCheckedChange = { scope.launch { dsm.editPreference(PrefKeys.commentPureMentionKey, it) } }) }
        item { SettingNumberListItem(title = "过滤 @ 人数达到阈值的评论", supportText = "0 表示关闭", value = limit.toDouble(), maxValue = 30.0,
            onValueChange = { scope.launch { dsm.editPreference(PrefKeys.commentMentionLimitKey, it.toInt()) } }) }
        item { SettingSwitchListItem(title = "过滤完整复制视频简介的评论", supportText = "只匹配完整简介，保留部分引用", checked = copy,
            onCheckedChange = { scope.launch { dsm.editPreference(PrefKeys.commentCopyIntroKey, it) } }) }
        item { Text("本地屏蔽 · ${ids.size} 个 UID") }
        item { Text("无需登录。过滤推荐、搜索、动态、评论和相关推荐。支持合并导入与复制导出。") }
        item {
            OutlinedTextField(
                value = viewModel.input, onValueChange = { viewModel.input = it },
                modifier = Modifier.fillMaxWidth().focusRequester(inputFocus)
                    .focusProperties { down = addFocus }
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.DirectionDown) {
                            if (event.type == KeyEventType.KeyDown) focusActions()
                            true
                        } else false
                    },
                enabled = !viewModel.saving,
                label = { Text("UID，可用逗号或换行分隔") }, minLines = 2, maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusActions() })
            )
        }
        item {
            Row(Modifier.focusProperties { up = inputFocus }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvToolbarAction("添加", modifier = Modifier.focusRequester(addFocus),
                    onClick = viewModel::add, available = !viewModel.saving && viewModel.input.isNotBlank())
                TvToolbarAction("合并导入", onClick = viewModel::import, available = !viewModel.saving && viewModel.input.isNotBlank())
                TvToolbarAction("导出", onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("本地屏蔽 UID", viewModel.export()))
                    "已复制屏蔽名单".toast(context)
                }, available = ids.isNotEmpty())
            }
        }
        viewModel.message?.let { item { Text(it) } }
        items(ids.sorted(), key = { it }) { id ->
            TvToolbarAction("UID $id · 解除屏蔽", modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.remove(id) }, available = !viewModel.saving)
        }
    }
}
