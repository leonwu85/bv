package dev.aaa1115910.bv.mobile.component.search

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.geetest.sdk.*
import dev.aaa1115910.bv.viewmodel.login.GeetestResult
import dev.aaa1115910.bv.viewmodel.search.SearchResultViewModel
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Composable
fun SearchVerification(viewModel: SearchResultViewModel) {
    val pending = viewModel.verification ?: return
    val context = LocalContext.current
    DisposableEffect(pending) {
        val sdk = GT3GeetestUtils(context)
        var submitted = false
        val config = GT3ConfigBean().apply {
            pattern = 1
            isCanceledOnTouchOutside = false
            timeout = 10000
            webviewTimeout = 10000
            corners = 24
            listener = object : GT3Listener() {
                override fun onReceiveCaptchaCode(p0: Int) {}
                override fun onStatistics(p0: String?) {}
                override fun onSuccess(p0: String?) {}
                override fun onButtonClick() {}
                override fun onClosed(p0: Int) { if (!submitted) viewModel.cancelVerification() }
                override fun onFailed(p0: GT3ErrorBean?) { if (!submitted) viewModel.cancelVerification() }
                override fun onDialogResult(result: String) {
                    runCatching { Json.decodeFromString<GeetestResult>(result) }.onSuccess {
                        submitted = true
                        sdk.showSuccessDialog()
                        viewModel.verify(it.geetestChallenge, it.geetestValidate, it.geetestSeccode, pending.challenge)
                    }.onFailure { sdk.showFailedDialog(); viewModel.cancelVerification() }
                }
            }
        }
        sdk.init(config)
        sdk.startCustomFlow()
        config.api1Json = JSONObject().apply { put("success", 1); put("gt", pending.gt); put("challenge", pending.challenge) }
        sdk.getGeetest()
        onDispose {
            sdk.destory()
            if (!submitted && viewModel.verification === pending) viewModel.cancelVerification()
        }
    }
}
