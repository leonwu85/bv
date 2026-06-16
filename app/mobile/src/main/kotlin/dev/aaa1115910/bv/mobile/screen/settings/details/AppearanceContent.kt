package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.mobile.theme.MobileThemePalette
import dev.aaa1115910.bv.mobile.theme.mobilePreviewColorScheme
import dev.aaa1115910.bv.mobile.R as MobileR

@Composable
fun AppearanceContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeType by MobilePrefs.themeTypeFlow.collectAsState(initial = MobilePrefs.themeType)
    val seedColor by MobilePrefs.seedColorFlow.collectAsState(initial = MobilePrefs.seedColor)
    val themePalette by MobilePrefs.themePaletteFlow.collectAsState(initial = MobilePrefs.themePalette)
    val dynamicColor by MobilePrefs.dynamicColorFlow.collectAsState(initial = MobilePrefs.dynamicColor)
    val lightBackgroundUri by MobilePrefs.lightThemeBackgroundUriFlow.collectAsState(
        initial = MobilePrefs.lightThemeBackgroundUri
    )
    val darkBackgroundUri by MobilePrefs.darkThemeBackgroundUriFlow.collectAsState(
        initial = MobilePrefs.darkThemeBackgroundUri
    )
    val isFixedPalette = themePalette != MobileThemePalette.MaterialDynamic
    var showCustomColorDialog by remember { mutableStateOf(false) }
    val lightBackgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            persistBackgroundUri(context, it) { persistedUri ->
                MobilePrefs.lightThemeBackgroundUri = persistedUri
            }
        }
    }
    val darkBackgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            persistBackgroundUri(context, it) { persistedUri ->
                MobilePrefs.darkThemeBackgroundUri = persistedUri
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    )
                )
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppearanceShowcase(
                themeType = themeType,
                themePalette = themePalette,
                seedColor = seedColor
            )
        }

        item {
            ScrollSectionCard(
                title = "主题模式",
                icon = when (themeType) {
                    ThemeType.Light -> Icons.Rounded.LightMode
                    ThemeType.Dark -> Icons.Rounded.DarkMode
                    ThemeType.Auto -> Icons.Rounded.Devices
                }
            ) {
                ThemeModeSelector(
                    selectedThemeType = themeType,
                    onThemeTypeChange = { MobilePrefs.themeType = it }
                )
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = "当前：${themeType.getDisplayName(context)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ScrollSectionCard(
                title = "主题配色",
                icon = Icons.Rounded.Palette,
                action = {
                    TextButton(
                        onClick = {
                            MobilePrefs.themeType = ThemeType.Auto
                            MobilePrefs.themePalette = MobileThemePalette.Default
                            MobilePrefs.dynamicColor = false
                            MobilePrefs.seedColor = MobilePrefs.DEFAULT_SEED_COLOR
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "重置")
                    }
                }
            ) {
                PaletteSelector(
                    selectedPalette = themePalette,
                    onPaletteChange = { MobilePrefs.themePalette = it }
                )
                DynamicColorRow(
                    modifier = Modifier.padding(top = 8.dp),
                    checked = dynamicColor,
                    enabled = !isFixedPalette,
                    onCheckedChange = { MobilePrefs.dynamicColor = it }
                )
            }
        }

        item {
            ScrollSectionCard(title = "预设色板", icon = Icons.Rounded.AutoAwesome) {
                PresetColorSelector(
                    selectedColor = seedColor,
                    enabled = !isFixedPalette,
                    onColorSelected = { MobilePrefs.seedColor = it }
                )
            }
        }

        item {
            ScrollSectionCard(title = "背景图片", icon = Icons.Rounded.Image) {
                BackgroundPickerRow(
                    title = "浅色背景",
                    summary = if (lightBackgroundUri.isBlank()) "使用内置手机/平板响应式浅色背景" else "已使用自定义浅色背景",
                    previewModel = lightBackgroundUri.takeIf { it.isNotBlank() }
                        ?: MobileR.drawable.mobile_bg_fantasy_scroll_light,
                    onPick = {
                        lightBackgroundPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onReset = { MobilePrefs.lightThemeBackgroundUri = "" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                BackgroundPickerRow(
                    title = "深色背景",
                    summary = if (darkBackgroundUri.isBlank()) "使用内置手机/平板响应式深色背景" else "已使用自定义深色背景",
                    previewModel = darkBackgroundUri.takeIf { it.isNotBlank() }
                        ?: MobileR.drawable.mobile_bg_fantasy_scroll_dark,
                    onPick = {
                        darkBackgroundPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onReset = { MobilePrefs.darkThemeBackgroundUri = "" }
                )
            }
        }

        item {
            ScrollSectionCard(title = "自定义主色", icon = Icons.Rounded.Palette) {
                CustomPrimaryColorRow(
                    color = seedColor,
                    enabled = !isFixedPalette,
                    onClick = { showCustomColorDialog = true }
                )
            }
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
private fun AppearanceShowcase(
    themeType: ThemeType,
    themePalette: MobileThemePalette,
    seedColor: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "外观设置",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "定制你的专属视觉体验",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ColorDot(
                    argb = MaterialTheme.colorScheme.primary.toArgb(),
                    selected = true,
                    size = 42.dp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                MiniPreviewPane(
                    modifier = Modifier.weight(1f),
                    label = "浅色模式",
                    colorScheme = mobilePreviewColorScheme(
                        palette = themePalette,
                        resolvedDarkTheme = false,
                        seedColor = Color(seedColor)
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                MiniPreviewPane(
                    modifier = Modifier.weight(1f),
                    label = "深色模式",
                    colorScheme = mobilePreviewColorScheme(
                        palette = themePalette,
                        resolvedDarkTheme = true,
                        seedColor = Color(seedColor)
                    )
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = when (themeType) {
                            ThemeType.Light -> Icons.Rounded.LightMode
                            ThemeType.Dark -> Icons.Rounded.DarkMode
                            ThemeType.Auto -> Icons.Rounded.Devices
                        },
                        contentDescription = null
                    )
                    Text(
                        text = when (themeType) {
                            ThemeType.Light -> "浅色模式"
                            ThemeType.Dark -> "深色模式"
                            ThemeType.Auto -> "跟随系统"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPreviewPane(
    modifier: Modifier = Modifier,
    label: String,
    colorScheme: ColorScheme
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "BV",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
            Surface(
                modifier = Modifier.weight(1f),
                color = colorScheme.surfaceContainer,
                contentColor = colorScheme.onSurfaceVariant,
                shape = CircleShape,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    text = "搜索",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "推荐",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.primary
            )
            Text(
                text = "热门",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = "番剧",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surfaceContainerLow,
            contentColor = colorScheme.onSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colorScheme.primaryContainer,
                                    colorScheme.tertiaryContainer,
                                    colorScheme.secondaryContainer
                                )
                            )
                        )
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        color = Color.Black.copy(alpha = 0.48f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            text = "08:24",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (label.startsWith("浅")) "云海之上" else "月下城邦",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun BackgroundPickerRow(
    title: String,
    summary: String,
    previewModel: Any,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .size(width = 72.dp, height = 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            model = previewModel,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(onClick = onPick) {
                    Text(text = "选择")
                }
                TextButton(onClick = onReset) {
                    Text(text = "恢复内置")
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selectedThemeType: ThemeType,
    onThemeTypeChange: (ThemeType) -> Unit
) {
    val options = listOf(
        ThemeModeOption(ThemeType.Light, "浅色", Icons.Rounded.LightMode),
        ThemeModeOption(ThemeType.Dark, "深色", Icons.Rounded.DarkMode),
        ThemeModeOption(ThemeType.Auto, "跟随", Icons.Rounded.Devices)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val selected = option.type == selectedThemeType
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onThemeTypeChange(option.type) },
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = option.icon,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSelector(
    selectedPalette: MobileThemePalette,
    onPaletteChange: (MobileThemePalette) -> Unit
) {
    val palettes = visibleThemePalettes()
    Column {
        palettes.forEachIndexed { index, palette ->
            PaletteOptionRow(
                palette = palette,
                selected = selectedPalette == palette,
                onClick = { onPaletteChange(palette) }
            )
            if (index != palettes.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun PaletteOptionRow(
    palette: MobileThemePalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            palettePreviewColors(palette).forEach { argb ->
                ColorDot(argb = argb, selected = false, size = 24.dp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = palette.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = paletteSummary(palette),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DynamicColorRow(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "动态取色",
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            Text(
                text = if (enabled) "根据壁纸智能生成主题色" else "固定配色会保持当前视觉风格",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PresetColorSelector(
    selectedColor: Int,
    enabled: Boolean,
    onColorSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MobileThemeColorPreset.entries.forEach { preset ->
            ColorDot(
                modifier = Modifier.clickable(enabled = enabled) { onColorSelected(preset.argb) },
                argb = preset.argb,
                selected = selectedColor == preset.argb,
                size = 46.dp,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun CustomPrimaryColorRow(
    color: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ColorDot(argb = color, selected = true, size = 42.dp, enabled = enabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "自定义品牌主色",
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            Text(
                text = if (enabled) "${color.toHexColor()} · 应用于按钮、导航与选中态"
                else "固定配色不使用自定义主色",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    ColorDot(argb = parsedColor ?: initialColor, selected = parsedColor != null)
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
    argb: Int,
    selected: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    enabled: Boolean = true
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(argb).copy(alpha = if (enabled) 1f else 0.38f))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .padding(if (selected) 4.dp else 0.dp)
            .clip(shape)
            .background(Color(argb).copy(alpha = if (enabled) 1f else 0.38f))
    )
}

private data class ThemeModeOption(
    val type: ThemeType,
    val label: String,
    val icon: ImageVector
)

private enum class MobileThemeColorPreset(val label: String, val argb: Int) {
    ScrollTeal("云青", MobilePrefs.DEFAULT_SEED_COLOR),
    BiliPink("Bilibili 粉", 0xFFFF6699.toInt()),
    ClearBlue("清蓝", 0xFF4A90E2.toInt()),
    Violet("浅紫", 0xFF8B6FD7.toInt()),
    Coral("珊瑚", 0xFFE96D5A.toInt()),
    Sunset("暖橙", 0xFFFF9F3A.toInt()),
    Leaf("草木", 0xFF7CA642.toInt()),
    Amber("琥珀", 0xFFB88146.toInt());
}

private fun palettePreviewColors(palette: MobileThemePalette): List<Int> = when (palette) {
    MobileThemePalette.Default,
    MobileThemePalette.FantasyScroll -> listOf(0xFF2F8F8F.toInt(), 0xFFA7782E.toInt(), 0xFFFFF4E2.toInt())
    MobileThemePalette.ChineseTraditional -> listOf(0xFF106697.toInt(), 0xFF037C63.toInt(), 0xFFD5B112.toInt())
    MobileThemePalette.MaterialDynamic -> listOf(0xFF2F8F8F.toInt(), 0xFF4A90E2.toInt(), 0xFFFF6699.toInt())
}

private fun paletteSummary(palette: MobileThemePalette): String = when (palette) {
    MobileThemePalette.Default,
    MobileThemePalette.FantasyScroll -> "卷轴米白、云青主色与细金点缀"
    MobileThemePalette.ChineseTraditional -> "青绿、陶土与金色的固定配色"
    MobileThemePalette.MaterialDynamic -> "支持动态取色和自定义主色"
}

private fun visibleThemePalettes(): List<MobileThemePalette> = listOf(
    MobileThemePalette.Default,
    MobileThemePalette.ChineseTraditional,
    MobileThemePalette.MaterialDynamic
)

private fun persistBackgroundUri(
    context: Context,
    uri: Uri,
    onPersisted: (String) -> Unit
) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
    onPersisted(uri.toString())
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
