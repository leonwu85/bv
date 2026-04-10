package dev.aaa1115910.bv.tv.screens.main

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.activities.settings.SettingsActivity
import dev.aaa1115910.bv.util.isDpadLeft
import dev.aaa1115910.bv.util.isKeyDown

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    onRequestDrawerFocus: () -> Unit = {},
) {
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
            },
        topBar = {
            Box(
                modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_activity_settings),
                    fontSize = 48.sp
                )
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.main_settings_content_prompt),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    modifier = Modifier.focusRequester(navFocusRequester),
                    onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                ) {
                    Text(text = stringResource(R.string.main_settings_open_button))
                }
            }
        }
    }
}