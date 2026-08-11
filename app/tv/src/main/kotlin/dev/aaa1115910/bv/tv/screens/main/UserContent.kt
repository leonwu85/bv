package dev.aaa1115910.bv.tv.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import dev.aaa1115910.bv.tv.screens.user.UserInfoScreen
import dev.aaa1115910.bv.viewmodel.UserViewModel

@Composable
fun UserContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    onRequestDrawerFocus: () -> Unit = {},
    userViewModel: UserViewModel,
) {
    UserInfoScreen(
        modifier = modifier.fillMaxSize(),
        initialFocusRequester = navFocusRequester,
        autoRequestInitialFocus = false,
        onRequestDrawerFocus = onRequestDrawerFocus,
        userViewModel = userViewModel,
    )
}
