package dev.aaa1115910.bv.component

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.CancellationException

/** The historical article endpoint returns HTML; scripting and local-file access stay disabled. */
@Composable
fun LegacyArticle(id: Long, tv: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var title by remember(id) { mutableStateOf("专栏") }
    var html by remember(id) { mutableStateOf<String?>(null) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var retry by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id, retry) {
        error = null
        try {
            val article = BiliHttpApi.getArticleView(id.toString(), Prefs.sessData).getResponseData()
            title = article.title ?: "专栏"
            html = article.content?.takeIf { it.isNotBlank() } ?: article.summary ?: "暂无正文"
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "专栏加载失败" }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            OpusAction("返回", tv, onClick = onBack)
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
            error?.let { Text(it); OpusAction("重试", tv) { retry++ } }
            val body = html
            if (body == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (body != null) AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            request?.url?.toString()?.let(onOpen)
                            return true
                        }
                    }
                    loadDataWithBaseURL("https://www.bilibili.com/", "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style>body{font-size:18px;line-height:1.7;padding:12px;color:#333}img{max-width:100%;height:auto}blockquote{border-left:3px solid #777;padding-left:16px}</style>$body", "text/html", "UTF-8", null)
                }
            }, onRelease = { it.destroy() })
        }
    }
}
