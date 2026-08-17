package dev.aaa1115910.bv.tv.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.tv.util.requireTvActivity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.repositories.SendSmsState
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.GeetestTvVerifyDialog
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.login.SmsLoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

private data class LoginCaptchaPrompt(
    val gt: String,
    val challenge: String,
)

@Composable
fun SmsLoginContent(
    modifier: Modifier = Modifier,
    smsLoginViewModel: SmsLoginViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = requireTvActivity()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var phoneNumberText by remember { mutableStateOf("") }
    var codeText by remember { mutableStateOf("") }
    var captchaPrompt by remember { mutableStateOf<LoginCaptchaPrompt?>(null) }
    var sendingSms by remember { mutableStateOf(false) }
    var loggingIn by remember { mutableStateOf(false) }

    fun requestSms(phone: Long) {
        if (sendingSms) return
        sendingSms = true
        scope.launch(Dispatchers.IO) {
            try {
                smsLoginViewModel.sendSms(phone) { challenge, gt ->
                    scope.launch(Dispatchers.Main) {
                        captchaPrompt = LoginCaptchaPrompt(gt = gt, challenge = challenge)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) { sendingSms = false }
            }
        }
    }

    fun sendSms() {
        keyboardController?.hide()
        val phone = phoneNumberText.takeIf { it.length == 11 }?.toLongOrNull()
        if (phone == null) {
            R.string.sms_login_invalid_phone.toast(context)
            return
        }
        requestSms(phone)
    }

    fun loginWithSms() {
        keyboardController?.hide()
        if (smsLoginViewModel.sendSmsState != SendSmsState.Success) {
            R.string.sms_login_toast_send_sms_first.toast(context)
            return
        }
        val code = codeText.toIntOrNull()
        if (code == null || codeText.length != 6) {
            R.string.sms_login_invalid_code.toast(context)
            return
        }
        if (loggingIn) return
        loggingIn = true
        scope.launch(Dispatchers.IO) {
            try {
                smsLoginViewModel.loginWithSms(code) {
                    activity.finish()
                }
            } finally {
                withContext(Dispatchers.Main) { loggingIn = false }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.sms_login_title),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(R.string.sms_login_phone_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier.width(420.dp),
                    value = phoneNumberText,
                    onValueChange = { value ->
                        phoneNumberText = value.filter(Char::isDigit).take(11)
                        captchaPrompt = null
                        smsLoginViewModel.clearCaptchaData()
                    },
                    label = { Text(text = stringResource(R.string.sms_login_phone_number)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { sendSms() })
                )
                Button(
                    enabled = !sendingSms && phoneNumberText.length == 11,
                    onClick = { sendSms() }
                ) {
                    Text(
                        text = stringResource(
                            if (sendingSms) R.string.sms_login_sending
                            else R.string.sms_login_button_send_sms
                        )
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier.width(420.dp),
                    value = codeText,
                    onValueChange = { codeText = it.filter(Char::isDigit).take(6) },
                    label = { Text(text = stringResource(R.string.sms_login_code)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { loginWithSms() })
                )
                Button(
                    enabled = !loggingIn && codeText.length == 6,
                    onClick = { loginWithSms() }
                ) {
                    Text(
                        text = stringResource(
                            if (loggingIn) R.string.sms_login_logging_in
                            else R.string.sms_login_button_login
                        )
                    )
                }
            }
            if (smsLoginViewModel.sendSmsState == SendSmsState.Success) {
                Text(
                    text = stringResource(R.string.sms_login_code_sent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    captchaPrompt?.let { prompt ->
        GeetestTvVerifyDialog(
            gt = prompt.gt,
            challenge = prompt.challenge,
            onResult = { result ->
                // Ignore a delayed result from a challenge that has already been refreshed.
                if (result.sourceChallenge != captchaPrompt?.challenge) return@GeetestTvVerifyDialog
                val phone = phoneNumberText.toLongOrNull()
                if (phone == null) {
                    captchaPrompt = null
                    smsLoginViewModel.clearCaptchaData()
                    return@GeetestTvVerifyDialog
                }
                smsLoginViewModel.applyGeetestResult(
                    challenge = result.challenge,
                    validate = result.validate,
                    seccode = result.seccode,
                )
                captchaPrompt = null
                requestSms(phone)
            },
            onDismiss = {
                captchaPrompt = null
                smsLoginViewModel.clearCaptchaData()
            },
            onRefreshChallenge = {
                val captcha = smsLoginViewModel.refreshCaptchaChallenge()
                if (captcha != null) {
                    captchaPrompt = LoginCaptchaPrompt(
                        gt = captcha.gt,
                        challenge = captcha.challenge,
                    )
                    true
                } else if (smsLoginViewModel.sendSmsState == SendSmsState.Success) {
                    // 刷新 challenge 时短信接口可能直接放行并发送验证码，此时无需再验证。
                    captchaPrompt = null
                    true
                } else {
                    false
                }
            },
        )
    }
}
