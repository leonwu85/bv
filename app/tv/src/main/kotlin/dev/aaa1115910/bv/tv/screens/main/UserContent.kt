package dev.aaa1115910.bv.tv.screens.main

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.activities.user.LoginActivity
import dev.aaa1115910.bv.tv.screens.user.UserInfoScreen
import dev.aaa1115910.bv.util.isDpadLeft
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.viewmodel.UserViewModel

@Composable
fun UserContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    onRequestDrawerFocus: () -> Unit = {},
    userViewModel: UserViewModel,
) {
    if (userViewModel.isLogin) {
        UserInfoScreen(
            modifier = modifier.fillMaxSize(),
            initialFocusRequester = navFocusRequester,
            autoRequestInitialFocus = false,
            onRequestDrawerFocus = onRequestDrawerFocus,
            userViewModel = userViewModel,
        )
        return
    }

    val context = LocalContext.current
    var contentHasFocus by remember { mutableStateOf(false) }

    BackHandler(enabled = contentHasFocus) {
        onRequestDrawerFocus()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .onFocusChanged { contentHasFocus = it.hasFocus }
            .onKeyEvent {
                if (contentHasFocus && it.isKeyDown() && it.isDpadLeft()) {
                    onRequestDrawerFocus()
                    true
                } else {
                    false
                }
            }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 48.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.58f),
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(horizontal = 36.dp, vertical = 40.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.title_activity_user_info),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 36.sp)
                        )
                        Text(
                            text = "", //stringResource(R.string.user_homepage_quick_access_tag)
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.main_user_login_prompt),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp)
                        )
                        Text(
                            text = stringResource(R.string.main_user_login_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            modifier = Modifier.focusRequester(navFocusRequester),
                            onClick = {
                                context.startActivity(Intent(context, LoginActivity::class.java))
                            }
                        ) {
                            Text(text = stringResource(R.string.sms_login_button_login))
                        }
                    }
                }
            }
        }
    }
}