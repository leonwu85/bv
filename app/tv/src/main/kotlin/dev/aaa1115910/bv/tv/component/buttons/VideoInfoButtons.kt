package dev.aaa1115910.bv.tv.component.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.formatHourMinSec

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoInfoButtons(
    modifier: Modifier = Modifier,
    playButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    commentButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    relatedButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    lastPlayedTime: Int = 0,
    isLogin: Boolean,
    onPlay: () -> Unit,
    // like
    isLike: Boolean,
    onAddLike: () -> Unit = {},
    onDelLike: () -> Unit = {},
    // coin
    isCoin: Boolean,
    onAddCoin: () -> Unit = {},
    // favorite
    isFavorite: Boolean,
    userFavoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    onAddToDefaultFavoriteFolder: () -> Unit = {},
    onUpdateFavoriteFolders: (List<Long>) -> Unit = {},
    // description
    hasDescription: Boolean = false,
    onShowDescription: () -> Unit = {},
    // comment
    onShowComment: () -> Unit = {},
    // related
    hasRelatedVideos: Boolean = false,
    onShowRelated: () -> Unit = {},
) {
    val outlinedColors = OutlinedButtonDefaults.colors()
    val outlinedBorder = OutlinedButtonDefaults.border()
    val outlinedContentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    val playButtonContentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 播放按钮（主按钮，填充色）
        Button(
            onClick = onPlay,
            modifier = Modifier
                .focusRequester(playButtonFocusRequester)
                .padding(end = 6.dp),
            contentPadding = playButtonContentPadding
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null
                )
                Text(
                    text = if (lastPlayedTime > 0) {
                        stringResource(
                            R.string.video_info_continue_play,
                            (lastPlayedTime * 1000L).formatHourMinSec()
                        )
                    } else {
                        stringResource(R.string.video_info_play)
                    }
                )
            }
        }

        // 点赞 / 收藏 / 投币
        if (isLogin) {
            LikeButton(
                isLike = isLike,
                onToggleLike = { if (isLike) onDelLike() else onAddLike() },
                colors = outlinedColors,
                border = outlinedBorder,
                contentPadding = outlinedContentPadding
            )
            FavoriteButton(
                isFavorite = isFavorite,
                userFavoriteFolders = userFavoriteFolders,
                favoriteFolderIds = favoriteFolderIds,
                onAddToDefaultFavoriteFolder = onAddToDefaultFavoriteFolder,
                onUpdateFavoriteFolders = onUpdateFavoriteFolders,
                colors = outlinedColors,
                border = outlinedBorder,
                contentPadding = outlinedContentPadding
            )
            CoinButton(
                isCoin = isCoin,
                onAddCoin = onAddCoin,
                colors = outlinedColors,
                border = outlinedBorder,
                contentPadding = outlinedContentPadding
            )
        }

        // 简介
        if (hasDescription) {
            IconButton(
                onClick = onShowDescription,
                colors = outlinedColors,
                border = outlinedBorder
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null
                )
            }
        }

        // 评论
        IconButton(
            modifier = Modifier.focusRequester(commentButtonFocusRequester),
            onClick = onShowComment,
            colors = outlinedColors,
            border = outlinedBorder
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.ChatBubbleOutline,
                contentDescription = null
            )
        }

        // 推荐
        if (hasRelatedVideos) {
            IconButton(
                modifier = Modifier.focusRequester(relatedButtonFocusRequester),
                onClick = onShowRelated,
                colors = outlinedColors,
                border = outlinedBorder
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Rounded.VideoLibrary,
                    contentDescription = null
                )
            }
        }
    }
}
