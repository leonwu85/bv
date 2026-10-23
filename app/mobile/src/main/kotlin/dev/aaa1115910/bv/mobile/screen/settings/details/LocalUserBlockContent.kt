package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.util.PrefKeys
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.LocalUserBlockViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LocalUserBlockContent(
    modifier: Modifier = Modifier,
    viewModel: LocalUserBlockViewModel = koinViewModel()
) {
    val ids by viewModel.blocked.collectAsState()
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        preferenceGroups("评论过滤" to {
        switchPreference(title = "过滤纯 @ 评论", summary = "仅隐藏只包含用户提及的评论", prefReq = PrefKeys.commentPureMentionRequest, onCheckedChange = { true })
        radioPreference(title = "过滤 @ 人数达到阈值的评论", prefReq = PrefKeys.commentMentionLimitRequest,
            values = listOf(0, 2, 3, 5, 10).associateWith { if (it == 0) "关闭" else "$it 人及以上" })
        switchPreference(title = "过滤完整复制视频简介的评论", summary = "只匹配完整简介，保留部分引用", prefReq = PrefKeys.commentCopyIntroRequest, onCheckedChange = { true })
        })
        item { Text("屏蔽名单保存在本机，无需登录。过滤推荐、搜索、动态、评论和相关推荐。", style = MaterialTheme.typography.bodyMedium) }
        item {
            OutlinedTextField(
                value = viewModel.input, onValueChange = { viewModel.input = it },
                modifier = Modifier.fillMaxWidth(), enabled = !viewModel.saving,
                label = { Text("UID，多个可用逗号或换行分隔") }, minLines = 2, maxLines = 4
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::add, enabled = !viewModel.saving && viewModel.input.isNotBlank()) { Text("添加") }
                OutlinedButton(onClick = viewModel::import, enabled = !viewModel.saving && viewModel.input.isNotBlank()) { Text("合并导入") }
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("本地屏蔽 UID", viewModel.export()))
                    "已复制屏蔽名单".toast(context)
                }, enabled = ids.isNotEmpty()) { Text("导出") }
            }
        }
        viewModel.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        item { Text("已屏蔽 ${ids.size} 个 UID", style = MaterialTheme.typography.titleMedium) }
        items(ids.sorted(), key = { it }) { id ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("UID $id", modifier = Modifier.padding(vertical = 12.dp))
                TextButton(onClick = { viewModel.remove(id) }, enabled = !viewModel.saving) { Text("解除屏蔽") }
            }
        }
    }
}
