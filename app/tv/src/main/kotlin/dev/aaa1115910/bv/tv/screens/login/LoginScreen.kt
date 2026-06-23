package dev.aaa1115910.bv.tv.screens.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.aaa1115910.bv.util.Prefs

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier
) {
    AppQRLoginContent(
        modifier = modifier,
        preferApiType = Prefs.apiType
    )
}
