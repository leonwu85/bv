package dev.aaa1115910.bv.tv.component.search

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.screens.search.SearchTheme
import dev.aaa1115910.bv.ui.theme.BVTheme

@Composable
fun SoftKeyboard(
    modifier: Modifier = Modifier,
    firstButtonFocusRequester: FocusRequester,
    showSearchWithProxy: Boolean,
    enableSearchWithProxy: Boolean,
    onClick: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onSearch: () -> Unit,
    onEnableSearchWithProxyChange: (Boolean) -> Unit
) {
    val keys = listOf(
        listOf("A", "B", "C", "D", "E", "F"),
        listOf("G", "H", "I", "J", "K", "L"),
        listOf("M", "N", "O", "P", "Q", "R"),
        listOf("S", "T", "U", "V", "W", "X"),
        listOf("Y", "Z", "1", "2", "3", "4"),
        listOf("5", "6", "7", "8", "9", "0")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        keys.forEachIndexed { rowIndex, rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                rowKeys.forEachIndexed { index, key ->
                    val keyModifier = if (rowIndex == 0 && index == 0) {
                        Modifier.focusRequester(firstButtonFocusRequester)
                    } else {
                        Modifier
                    }
                    SoftKeyboardKey(
                        modifier = keyModifier.weight(1f),
                        key = key,
                        onClick = { onClick(key) }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            SoftKeyboardButton(
                modifier = Modifier.weight(1f),
                key = stringResource(R.string.search_input_soft_keybord_clear),
                onClick = onClear
            )
            SoftKeyboardButton(
                modifier = Modifier.weight(1f),
                key = stringResource(R.string.search_input_soft_keybord_delete),
                onClick = onDelete
            )
            SoftKeyboardButton(
                modifier = Modifier.weight(1f),
                key = stringResource(R.string.search_input_soft_keybord_search),
                onClick = onSearch,
                isAccent = true
            )
        }
        if (showSearchWithProxy) {
            Surface(
                modifier = Modifier,
                onClick = { onEnableSearchWithProxyChange(!enableSearchWithProxy) },
                shape = ClickableSurfaceDefaults.shape(SearchTheme.keyShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                    pressedContainerColor = MaterialTheme.colorScheme.inverseSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableSearchWithProxy,
                        onCheckedChange = { onEnableSearchWithProxyChange(it) },
                    )
                    Text(text = "通过代理搜索")
                }
            }
        }
    }
}

@Composable
fun SoftKeyboardKey(
    modifier: Modifier = Modifier,
    key: String,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (hasFocus) 1.1f else 1.0f,
        animationSpec = tween(150),
        label = "key scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (hasFocus)
            MaterialTheme.colorScheme.inverseSurface
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(150),
        label = "key bg"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { hasFocus = it.hasFocus },
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(SearchTheme.keyShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = bgColor,
            focusedContainerColor = bgColor,
            pressedContainerColor = bgColor
        )
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun SoftKeyboardButton(
    modifier: Modifier = Modifier,
    key: String,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (hasFocus) 1.05f else 1.0f,
        animationSpec = tween(150),
        label = "btn scale"
    )

    Surface(
        modifier = modifier
            .height(36.dp)
            .scale(scale)
            .onFocusChanged { hasFocus = it.hasFocus },
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(SearchTheme.keyShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isAccent) Color.Transparent
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedContainerColor = if (isAccent) Color.Transparent
            else MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = if (isAccent) Color.Transparent
            else MaterialTheme.colorScheme.inverseSurface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isAccent) Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(SearchTheme.accentPink, SearchTheme.accentPurple)
                        ),
                        SearchTheme.keyShape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelLarge,
                color = if (isAccent) Color.White else Color.Unspecified
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SoftKeyboardKeyPreview() {
    BVTheme {
        SoftKeyboardKey(
            key = "X",
            onClick = {}
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SoftKeyboardPreview() {
    val firstButtonFocusRequester = remember { FocusRequester() }
    BVTheme {
        SoftKeyboard(
            firstButtonFocusRequester = firstButtonFocusRequester,
            showSearchWithProxy = true,
            enableSearchWithProxy = true,
            onClick = {},
            onClear = {},
            onDelete = {},
            onSearch = {},
            onEnableSearchWithProxyChange = {}
        )
    }
}