package dev.aaa1115910.bv.tv.screens.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.Prefs

private enum class TvLoginMethod {
    QrCode,
    Sms,
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier
) {
    val preferredApiType = Prefs.apiType
    var selectedMethod by remember { mutableStateOf(TvLoginMethod.QrCode) }
    val methods = TvLoginMethod.entries

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TabRow(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .width(520.dp),
                selectedTabIndex = methods.indexOf(selectedMethod),
            ) {
                methods.forEach { method ->
                    val title = when (method) {
                        TvLoginMethod.QrCode -> stringResource(
                            if (preferredApiType == ApiType.Web) R.string.login_method_web_qr
                            else R.string.login_method_app_qr
                        )

                        TvLoginMethod.Sms -> stringResource(R.string.login_method_sms)
                    }
                    Tab(
                        selected = method == selectedMethod,
                        onFocus = { selectedMethod = method },
                        onClick = { selectedMethod = method },
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                when (selectedMethod) {
                    TvLoginMethod.QrCode -> AppQRLoginContent(
                        modifier = Modifier.fillMaxSize(),
                        preferApiType = preferredApiType,
                    )

                    TvLoginMethod.Sms -> SmsLoginContent(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
