package dev.aaa1115910.bv.component.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.QrImage
import dev.aaa1115910.bv.network.GithubApi

@Composable
fun GithubRateLimitContent(
    text: @Composable (String) -> Unit,
    modifier: Modifier = Modifier,
    qrSize: Dp = 240.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            text(stringResource(R.string.github_rate_limit_dialog_message))
        }
        QrImage(
            modifier = Modifier.size(qrSize),
            content = GithubApi.RELEASES_URL,
            contentDescription = stringResource(
                R.string.github_rate_limit_qr_content_description,
            ),
            borderWidth = 16.dp,
            showLoadingWhenContentChanged = false,
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            text(stringResource(R.string.github_rate_limit_dialog_url))
        }
    }
}
