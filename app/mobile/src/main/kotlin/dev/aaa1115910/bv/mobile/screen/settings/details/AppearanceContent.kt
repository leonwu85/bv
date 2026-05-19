package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.textPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.mobile.theme.MobileThemePalette

@Composable
fun AppearanceContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val seedColor by MobilePrefs.seedColorFlow.collectAsState(initial = MobilePrefs.seedColor)
    val themePalette by MobilePrefs.themePaletteFlow.collectAsState(initial = MobilePrefs.themePalette)
    val isFixedPalette = themePalette == MobileThemePalette.ChineseTraditional
    var showCustomColorDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
    ) {
        preferenceGroups(
            "主题" to {
                radioPreference(
                    title = "主题模式",
                    prefReq = MobilePrefKeys.themeTypeRequest,
                    values = ThemeType.entries.associate { it.ordinal to it.getDisplayName(context) }
                )
                radioPreference(
                    title = "主题配色",
                    value = themePalette,
                    values = MobileThemePalette.entries.associateWith { it.displayName },
                    onValueChange = { MobilePrefs.themePalette = it }
                )
                switchPreference(
                    title = "动态取色",
                    summary = if (isFixedPalette) "中国传统色使用固定配色" else "Android 12 及以上优先使用系统动态色",
                    enabled = !isFixedPalette,
                    prefReq = MobilePrefKeys.dynamicColorRequest,
                    onCheckedChange = { true }
                )
            },
            "主色" to {
                radioPreference(
                    title = "预设色板",
                    leadingContent = {
                        ColorDot(argb = seedColor)
                    },
                    enabled = !isFixedPalette,
                    value = seedColor,
                    values = MobileThemeColorPreset.valuesWithCustom(seedColor),
                    onValueChange = { MobilePrefs.seedColor = it }
                )
                textPreference(
                    title = "自定义主色",
                    summary = if (isFixedPalette) "中国传统色主题不使用自定义主色" else seedColor.toHexColor(),
                    leadingContent = {
                        Icon(imageVector = Icons.Rounded.Palette, contentDescription = null)
                    },
                    enabled = !isFixedPalette,
                    onClick = { showCustomColorDialog = true }
                )
            }
        )

        item {
            ThemePreview(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 16.dp)
                    .fillMaxWidth()
            )
        }
    }

    if (showCustomColorDialog) {
        CustomColorDialog(
            initialColor = seedColor,
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { color ->
                MobilePrefs.seedColor = color
                showCustomColorDialog = false
            }
        )
    }
}

@Composable
private fun ThemePreview(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColorDot(argb = colorScheme.primary.toArgb())
                Text(
                    text = "主题预览",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewChip(
                        modifier = Modifier.weight(1f),
                        label = "主色",
                        color = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                    PreviewChip(
                        modifier = Modifier.weight(1f),
                        label = "辅色",
                        color = colorScheme.secondary,
                        contentColor = colorScheme.onSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewChip(
                        modifier = Modifier.weight(1f),
                        label = "点睛",
                        color = colorScheme.tertiary,
                        contentColor = colorScheme.onTertiary
                    )
                    PreviewChip(
                        modifier = Modifier.weight(1f),
                        label = "底色",
                        color = colorScheme.surfaceContainerHighest,
                        contentColor = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewChip(
    modifier: Modifier = Modifier,
    label: String,
    color: Color,
    contentColor: Color
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            text = label
        )
    }
}

@Composable
private fun CustomColorDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var input by remember(initialColor) { mutableStateOf(initialColor.toHexColor()) }
    val parsedColor = remember(input) { input.parseHexColor() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "自定义主色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(text = "#RRGGBB") },
                    singleLine = true,
                    isError = parsedColor == null
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorDot(argb = parsedColor ?: initialColor)
                    Text(text = parsedColor?.toHexColor() ?: "颜色格式无效")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedColor != null,
                onClick = { parsedColor?.let(onConfirm) }
            ) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun ColorDot(
    modifier: Modifier = Modifier,
    argb: Int
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(argb))
    )
}

private enum class MobileThemeColorPreset(val label: String, val argb: Int) {
    Green("松石绿", MobilePrefs.DEFAULT_SEED_COLOR),
    Pink("Bilibili 粉", 0xFFFF6699.toInt()),
    Blue("清蓝", 0xFF4A90E2.toInt()),
    Orange("暖橙", 0xFFFF8A4C.toInt()),
    Violet("浅紫", 0xFF8B6FD7.toInt());

    companion object {
        fun valuesWithCustom(currentColor: Int): Map<Int, String> {
            val presets = entries.associate { it.argb to it.label }.toMutableMap()
            if (currentColor !in presets.keys) {
                presets[currentColor] = "自定义 ${currentColor.toHexColor()}"
            }
            return presets
        }
    }
}

private fun Int.toHexColor(): String = "#%06X".format(this and 0x00FFFFFF)

private fun String.parseHexColor(): Int? {
    val normalized = trim()
        .removePrefix("#")
        .removePrefix("0x")
        .removePrefix("0X")
    val argbString = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized
        else -> return null
    }
    return argbString.toLongOrNull(radix = 16)?.toInt()
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppearanceContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            AppearanceContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
