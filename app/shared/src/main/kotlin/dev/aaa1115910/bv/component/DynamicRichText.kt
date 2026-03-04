package dev.aaa1115910.bv.component

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.user.RichTextNode
import dev.aaa1115910.biliapi.entity.user.RichTextNodeType

/**
 * 支持表情图片的富文本组件
 *
 * 将 [RichTextNode] 列表中的表情节点渲染为内联图片，其余渲染为普通文本。
 * 当 [richTextNodes] 为空时，回退到显示 [fallbackText] 纯文本。
 *
 * @param richTextNodes 富文本节点列表，包含文本和表情信息
 * @param fallbackText 当 richTextNodes 为空时的回退纯文本
 * @param modifier Modifier
 * @param style 文本样式
 * @param fontSize 文字大小
 */
@Composable
fun DynamicRichText(
    richTextNodes: List<RichTextNode>,
    fallbackText: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontSize: TextUnit = 16.sp
) {
    // 如果没有富文本节点，回退到纯文本
    if (richTextNodes.isEmpty()) {
        Text(
            text = fallbackText,
            modifier = modifier,
            style = style,
            fontSize = fontSize
        )
        return
    }

    // 检查是否有表情节点
    val hasEmoji = richTextNodes.any { it.type == RichTextNodeType.Emoji && it.emoji != null }

    if (!hasEmoji) {
        // 没有表情，直接拼接文本显示
        Text(
            text = richTextNodes.joinToString("") { it.text },
            modifier = modifier,
            style = style,
            fontSize = fontSize
        )
        return
    }

    // 构建表情的 InlineTextContent 映射
    val emojiNodes = richTextNodes.filter { it.type == RichTextNodeType.Emoji && it.emoji != null }

    val inlineContentMap = emojiNodes.associate { node ->
        val emojiSize = if (node.emoji!!.size >= 2) {
            // 大表情
            (fontSize.value * 2).sp
        } else {
            // 小表情（行内）
            (fontSize.value * 1.25f).sp
        }
        node.text to InlineTextContent(
            Placeholder(
                width = emojiSize,
                height = emojiSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(
                model = node.emoji!!.iconUrl,
                contentDescription = node.text
            )
        }
    }

    // 构建 AnnotatedString
    val annotatedString = buildAnnotatedString {
        richTextNodes.forEach { node ->
            when (node.type) {
                RichTextNodeType.Emoji -> {
                    if (node.emoji != null) {
                        appendInlineContent(node.text)
                    } else {
                        append(node.text)
                    }
                }

                else -> append(node.text)
            }
        }
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContentMap,
        modifier = modifier,
        style = style,
        fontSize = fontSize
    )
}
