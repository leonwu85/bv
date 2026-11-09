package dev.aaa1115910.bv.mobile.component

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import dev.aaa1115910.bv.network.buildGaiaCaptchaHtml
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GaiaVerificationDialog(voucher: String, onVerified: (String, String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        key(voucher) {
            val scope = rememberCoroutineScope()
            val callback = rememberUpdatedState(onVerified)
            AndroidView(
                modifier = Modifier.padding(vertical = 24.dp).fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onGaiaVerified(token: String?) {
                                if (!token.isNullOrBlank()) scope.launch { callback.value(voucher, token) }
                            }
                        }, "Android")
                        loadDataWithBaseURL("https://api.bilibili.com/", buildGaiaCaptchaHtml(voucher), "text/html", "utf-8", null)
                    }
                },
                onRelease = { it.destroy() },
            )
        }
    }
}
