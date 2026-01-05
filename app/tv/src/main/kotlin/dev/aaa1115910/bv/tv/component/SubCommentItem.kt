package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.bv.util.focusedBorder

/**
 * 子评论项组件
 *
 * 子评论有焦点边框，但不响应点击
 *
 * @param comment 评论数据
 * @param modifier 修饰符
 */
@Composable
fun SubCommentItem(
    comment: Comment,
    modifier: Modifier = Modifier
) {
    // 子评论有焦点边框，但不响应点击
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .focusedBorder(MaterialTheme.shapes.small),
        onClick = { /* 空回调，不执行任何操作 */ },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f,
            pressedScale = 1f
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            // 头像
            AsyncImage(
                model = comment.member.avatar,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            // 内容
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 用户名
                Text(
                    text = comment.member.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // 评论内容 - 拼接后显示，支持自然换行
                Text(
                    text = comment.content.joinToString(""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                // 底部信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = comment.timeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${comment.like} 赞",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 子评论根评论显示组件（只读，不可点击）
 *
 * @param comment 评论数据
 */
@Composable
fun SubCommentRootItem(
    comment: Comment
) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            AsyncImage(
                model = comment.member.avatar,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = comment.member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // 评论内容 - 拼接后显示，支持自然换行
                Text(
                    text = comment.content.joinToString(""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = comment.timeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${comment.like} 赞",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
//                    Text(
//                        text = "${comment.rcount} 回复",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = Color.White.copy(alpha = 0.5f)
//                    )
                }
            }
        }
    }
}
