package dev.aaa1115910.bv.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.backup.BackupRepository
import dev.aaa1115910.bv.backup.LoadedBackup
import dev.aaa1115910.bv.repository.LocalUserBlockRepository
import dev.aaa1115910.bv.util.AppRestarter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File

@Composable
fun BackupSettings(tv: Boolean = false) {
    val context = LocalContext.current
    val blocks = getKoin().get<LocalUserBlockRepository>()
    val repository = remember(context, blocks) { BackupRepository(context.applicationContext, blocks) }
    val scope = rememberCoroutineScope()
    var settings by rememberSaveable { mutableStateOf(true) }
    var accounts by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(true) }
    var blocked by rememberSaveable { mutableStateOf(true) }
    var pendingSelection by remember { mutableStateOf(BackupRepository.Selection()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf<LoadedBackup?>(null) }
    var localFiles by remember { mutableStateOf<List<File>?>(null) }
    val localStorage = remember(context, tv) {
        tv && listOf(Intent.ACTION_CREATE_DOCUMENT, Intent.ACTION_OPEN_DOCUMENT).any { action ->
            val intent = Intent(action).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip")
            val handler = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            handler == null || handler.activityInfo.packageName.contains("frameworkpackagestubs")
        }
    }
    val selection = BackupRepository.Selection(settings, accounts, history, blocked)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) { busy = false } else scope.launch {
            try { repository.export(uri, pendingSelection); message = "备份已保存" }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "备份失败，请检查存储空间、文件权限和自定义主题资源" }
            finally { busy = false }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { busy = false } else scope.launch {
            try { loaded = repository.read(uri); message = null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "无法读取备份：文件损坏、格式不兼容或内容校验失败" }
            finally { busy = false }
        }
    }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("备份与恢复", style = MaterialTheme.typography.titleMedium)
        Text("选择要导出或恢复的数据。设置包含两端配置、自定义背景和字体；视频缓存与临时文件不打包。", style = MaterialTheme.typography.bodyMedium)
        if (localStorage) Text("本机备份目录：${repository.localDirectory.absolutePath}\n可将其他设备的 BV ZIP 备份复制到此目录后恢复。", style = MaterialTheme.typography.bodySmall)
        @Composable fun option(label: String, value: Boolean, change: () -> Unit) {
            if (tv) OpusAction("${if (value) "☑" else "☐"} $label", true) { if (!busy) change() }
            else FilterChip(selected = value, enabled = !busy, onClick = change, label = { Text(label) })
        }
        option("设置与主题资源", settings) { settings = !settings }
        option("账号与登录状态", accounts) { accounts = !accounts }
        option("搜索历史", history) { history = !history }
        option("本地屏蔽名单", blocked) { blocked = !blocked }
        if (accounts) Text("账号备份包含登录凭据，请妥善保管文件。", style = MaterialTheme.typography.bodySmall)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OpusAction("导出 ZIP", tv) {
                if (!busy && (settings || accounts || history || blocked)) {
                    pendingSelection = selection; busy = true
                    if (localStorage) scope.launch {
                        try { message = "已保存：${repository.exportLocal(pendingSelection).name}" }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { message = "备份失败，请检查存储空间和主题资源" }
                        finally { busy = false }
                    } else runCatching { export.launch("BV-backup-${System.currentTimeMillis()}.zip") }
                        .onFailure { busy = false; message = "系统没有可用的文件选择器" }
                }
            }
            OpusAction("选择备份恢复", tv) {
                if (!busy && (settings || accounts || history || blocked)) {
                    busy = true
                    if (localStorage) scope.launch {
                        try { localFiles = repository.localBackups() }
                        catch (_: Exception) { message = "无法读取本机备份目录" }
                        finally { busy = false }
                    } else runCatching { import.launch(arrayOf("application/zip", "application/octet-stream")) }
                        .onFailure { busy = false; message = "系统没有可用的文件选择器" }
                }
            }
        }
        message?.let { Text(it) }
    }
    localFiles?.let { files ->
        AlertDialog(onDismissRequest = { if (!busy) localFiles = null }, title = { Text("选择本机备份") },
            text = {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    if (files.isEmpty()) item { Text("暂无备份。请先导出，或将 BV ZIP 文件复制到本机备份目录。") }
                    items(files, key = { it.absolutePath }) { file ->
                        OpusAction(file.name, tv) {
                            if (!busy) scope.launch {
                                busy = true
                                try { loaded = repository.read(Uri.fromFile(file)); localFiles = null; message = null }
                                catch (e: CancellationException) { throw e }
                                catch (_: Exception) { message = "备份格式不兼容或内容校验失败"; localFiles = null }
                                finally { busy = false }
                            }
                        }
                    }
                }
            }, confirmButton = { OpusAction("取消", tv) { if (!busy) localFiles = null } })
    }
    loaded?.let { backup ->
        val doc = backup.document
        val summary = buildList {
            if (settings && doc.settings != null) add("设置 ${doc.settings.size} 项、主题资源 ${doc.assets.size} 个")
            if (accounts && doc.accounts != null) add("账号 ${doc.accounts.size} 个（含登录状态）")
            if (history && doc.history != null) add("搜索历史 ${doc.history.size} 条")
            if (blocked && doc.blockedUsers != null) add("本地屏蔽 ${doc.blockedUsers.size} 项")
        }
        AlertDialog(onDismissRequest = { if (!busy) loaded = null }, title = { Text("确认恢复备份") },
            text = { Text(if (summary.isEmpty()) "备份中没有勾选的数据类别，请取消并调整选择。" else summary.joinToString("\n") + "\n\n将覆盖以上本地数据，完成后自动重启应用。") },
            confirmButton = { OpusAction(if (busy) "恢复中…" else "恢复并重启", tv) {
                if (!busy && summary.isNotEmpty()) scope.launch {
                    busy = true
                    try { repository.restore(backup, selection); loaded = null; AppRestarter.restart(context) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { message = "恢复失败，已尝试回滚，请检查存储空间后重试"; loaded = null }
                    finally { busy = false }
                }
            } },
            dismissButton = { OpusAction("取消", tv) { if (!busy) loaded = null } })
    }
}
