package dev.aaa1115910.bv.tv.screens.login

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.tv.util.requireTvActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.login.QrLoginState
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.QrImage
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.login.AppQrLoginViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel

private const val TvQrRefreshCountdownSeconds = 180

@Composable
fun AppQRLoginContent(
    modifier: Modifier = Modifier,
    appQrLoginViewModel: AppQrLoginViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = requireTvActivity()
    var refreshCountdown by remember { mutableIntStateOf(TvQrRefreshCountdownSeconds) }
    val qrVisible = listOf(QrLoginState.WaitingForScan, QrLoginState.WaitingForConfirm)
        .contains(appQrLoginViewModel.state) && appQrLoginViewModel.loginUrl.isNotBlank()
    val requestQrCode = {
        refreshCountdown = TvQrRefreshCountdownSeconds
        appQrLoginViewModel.requestQRCode(
            preferApiType = ApiType.App
        )
    }

    LaunchedEffect(Unit) {
        requestQrCode()
    }

    LaunchedEffect(qrVisible, appQrLoginViewModel.loginUrl) {
        if (!qrVisible) return@LaunchedEffect

        var remainingSeconds = TvQrRefreshCountdownSeconds
        refreshCountdown = remainingSeconds
        while (remainingSeconds > 0 && isActive) {
            delay(1000)
            remainingSeconds -= 1
            refreshCountdown = remainingSeconds
        }

        if (isActive) {
            requestQrCode()
        }
    }

    LaunchedEffect(appQrLoginViewModel.state) {
        when (appQrLoginViewModel.state) {
            QrLoginState.Success -> {
                R.string.login_success.toast(context)
                activity.finish()
            }

            QrLoginState.Expired -> {
                requestQrCode()
            }

            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            appQrLoginViewModel.cancelCheckLoginResultTimer()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = modifier
                .focusable()
                .fillMaxSize()
                .onKeyEvent {
                    if (it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        if (listOf(QrLoginState.Expired, QrLoginState.Error)
                                .contains(appQrLoginViewModel.state)
                        ) {
                            requestQrCode()
                        }
                        return@onKeyEvent true
                    }
                    false
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                AnimatedVisibility(
                    visible = qrVisible
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "$refreshCountdown 秒后自动刷新二维码",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                            textAlign = TextAlign.Center
                        )
                        QrImage(
                            modifier = Modifier.size(240.dp),
                            content = appQrLoginViewModel.loginUrl
                        )
                        Text(
                            modifier = Modifier.widthIn(max = 720.dp),
                            text = appQrLoginViewModel.loginUrl,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = when (appQrLoginViewModel.state) {
                            QrLoginState.Ready, QrLoginState.RequestingQRCode -> stringResource(R.string.login_requesting)
                            QrLoginState.WaitingForScan -> stringResource(R.string.login_wait_for_scan)
                            QrLoginState.WaitingForConfirm -> stringResource(R.string.login_wait_for_confirm)
                            QrLoginState.Expired -> stringResource(R.string.login_expired)
                            QrLoginState.Success -> stringResource(R.string.login_success)
                            QrLoginState.Error, QrLoginState.Unknown -> stringResource(R.string.login_error)
                        },
                        style = MaterialTheme.typography.displaySmall,
                    )
                    AnimatedVisibility(
                        visible = listOf(QrLoginState.Expired, QrLoginState.Error)
                            .contains(appQrLoginViewModel.state)
                    ) {
                        Text(
                            text = stringResource(R.string.login_retry),
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 26.sp
                        )
                    }
                }
            }
        }
    }
}
