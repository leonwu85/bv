package dev.aaa1115910.bv.mobile.component.videocard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.BlurTransformation
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.mobile.theme.LocalVideoCardBlurBackgroundEnabled
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallVideoCard(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    onClick: () -> Unit = {},
    managementActionLabel: String? = null,
    onManagementAction: (() -> Unit)? = null,
    showMoreMenu: Boolean = true,
    enableUpNavigation: Boolean = true,
) {
    val context = LocalContext.current
    val showDanmakuCount = data.playString.isEmpty() || LocalDensity.current.fontScale <= 1.2f
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Box {
            if (LocalVideoCardBlurBackgroundEnabled.current) {
                SmallVideoCardBackground(
                    modifier = Modifier.matchParentSize(),
                    cover = data.cover
                )
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallVideoCardAuthor(
                        modifier = Modifier.weight(1f),
                        data = data,
                        enableNavigation = enableUpNavigation,
                        onClick = {
                            UserSpaceActivity.actionStart(context, data.upId, data.upName)
                        }
                    )
                    if (showMoreMenu) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            VideoCardMoreMenu(
                                modifier = Modifier.size(48.dp),
                                data = data,
                                managementActionLabel = managementActionLabel,
                                onManagementAction = onManagementAction
                            )
                        }
                    } else {
                        // Keep room for the collection selection indicator.
                        Spacer(Modifier.size(48.dp))
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        model = data.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                    if (data.coverBadges.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            data.coverBadges.forEach { badge ->
                                SmallVideoCardBadge(
                                    text = badge,
                                    isCharging = badge.contains("充电")
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp, 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (data.playString != "") {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        modifier = Modifier.size(14.dp),
                                        painter = painterResource(id = R.drawable.ic_play_count),
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Text(
                                        modifier = Modifier.weight(1f, fill = false),
                                        text = data.playString,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (data.danmakuString != "" && showDanmakuCount) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        modifier = Modifier.size(14.dp),
                                        painter = painterResource(id = R.drawable.ic_danmaku_count),
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Text(
                                        modifier = Modifier.weight(1f, fill = false),
                                        text = data.danmakuString,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = data.timeString,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    text = data.title,
                    style = MaterialTheme.typography.titleSmall,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SmallVideoCardBackground(
    modifier: Modifier = Modifier,
    cover: String
) {
    val context = LocalContext.current
    val surface = MaterialTheme.colorScheme.surfaceBright
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val request = remember(context, cover) {
        ImageRequest.Builder(context)
            .data(cover.resizedImageUrl(ImageSize.SmallVideoCardCover))
            // A small, cached bitmap keeps blur work out of scrolling/rendering.
            .size(160, 160)
            .allowHardware(false)
            .transformations(BlurTransformation(context.applicationContext, radius = 5f, sampling = 1f))
            .build()
    }
    Box(modifier = modifier) {
        AsyncImage(
            modifier = Modifier.matchParentSize(),
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(surface.copy(alpha = if (isDark) 0.78f else 0.68f))
        )
    }
}

@Composable
private fun SmallVideoCardAuthor(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    enableNavigation: Boolean,
    onClick: () -> Unit
) {
    val openSpaceLabel = stringResource(R.string.toview_menu_goto_up_space)
    val density = LocalDensity.current
    val largeText = density.fontScale > 1.2f
    val textMeasurer = rememberTextMeasurer()
    val nameStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Row(
        modifier = modifier
            .then(
                if (enableNavigation && data.upId > 0L) {
                    Modifier.clickable(
                        onClickLabel = openSpaceLabel,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    // Let the card handle the tap in collection selection mode.
                    Modifier
                }
            )
            .heightIn(min = 56.dp)
            .padding(start = 8.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (largeText) 6.dp else 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (largeText) 32.dp else 40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(26.dp),
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AsyncImage(
                modifier = Modifier.matchParentSize(),
                model = data.upFace,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            // Resolve wrapping before composing the icon so the header does not jump a frame.
            val nameLayout = textMeasurer.measure(
                text = data.upName,
                style = nameStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = constraints.maxWidth)
            )
            Column(
                modifier = Modifier.heightIn(
                    // Text layout accounts for Android's nonlinear font scaling.
                    min = with(density) {
                        (nameLayout.size.height * 2f / nameLayout.lineCount).toDp()
                    }
                ),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = data.upName,
                    style = nameStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (nameLayout.lineCount == 1) {
                    UpIcon(
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallVideoCardBadge(
    text: String,
    isCharging: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val containerColor = when {
        isCharging && isDark -> colorScheme.errorContainer
        isCharging -> colorScheme.error
        else -> colorScheme.primary
    }
    val contentColor = when {
        isCharging && isDark -> colorScheme.onErrorContainer
        isCharging -> colorScheme.onError
        else -> colorScheme.onPrimary
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun SmallVideoCardPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        isInteractive = true
    )
    BVMobileTheme {
        Surface {
            SmallVideoCard(
                data = data
            )
        }
    }
}

@Preview
@Composable
fun SmallVideoCardsPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        isInteractive = true
    )
    BVMobileTheme {
        Surface {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2)
            ) {
                repeat(20) {
                    item {
                        SmallVideoCard(
                            data = data
                        )
                    }
                }
            }
        }
    }
}
