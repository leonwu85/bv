package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.user.ArticleParagraph
import dev.aaa1115910.biliapi.entity.user.ArticleTextNode
import dev.aaa1115910.biliapi.entity.user.TextNodeType

/**
 * 专栏内容渲染组件
 * 用于渲染专栏文章的段落列表
 * 注意：此组件用于 LazyColumn 内部，因此使用 Column 而非 LazyColumn
 */
@Composable
fun ArticleContent(
    modifier: Modifier = Modifier,
    paragraphs: List<ArticleParagraph>
) {
    val linkColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        paragraphs.forEachIndexed { index, paragraph ->
            when (paragraph) {
                is ArticleParagraph.TextParagraph -> {
                    TextParagraphContent(
                        paragraph = paragraph,
                        linkColor = linkColor
                    )
                }
                is ArticleParagraph.PicturesParagraph -> {
                    PicturesParagraphContent(paragraph = paragraph)
                }
                is ArticleParagraph.LineParagraph -> {
                    LineParagraphContent(paragraph = paragraph)
                }
            }
        }
    }
}

/**
 * 文本段落渲染
 */
@Composable
private fun TextParagraphContent(
    paragraph: ArticleParagraph.TextParagraph,
    linkColor: Color
) {
    if (paragraph.nodes.isEmpty()) return

    val annotatedText = buildAnnotatedStringFromNodes(paragraph.nodes, linkColor)
    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * 图片段落渲染
 */
@Composable
private fun PicturesParagraphContent(
    paragraph: ArticleParagraph.PicturesParagraph
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        paragraph.pictures.forEach { picture ->
            if (picture.url.isNotBlank()) {
                AsyncImage(
                    model = picture.url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

/**
 * 分隔线段落渲染
 */
@Composable
private fun LineParagraphContent(
    paragraph: ArticleParagraph.LineParagraph
) {
    paragraph.picture?.let { pic ->
        if (pic.url.isNotBlank()) {
            AsyncImage(
                model = pic.url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(vertical = 8.dp),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

/**
 * 将文本节点列表转换为 AnnotatedString
 */
private fun buildAnnotatedStringFromNodes(
    nodes: List<ArticleTextNode>,
    linkColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        nodes.forEach { node ->
            when (node.type) {
                TextNodeType.Plain -> {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = if (node.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (node.isItalic) FontStyle.Italic else FontStyle.Normal
                        )
                    ) {
                        append(node.text)
                    }
                }
                TextNodeType.Link -> {
                    withStyle(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(node.text)
                    }
                }
                TextNodeType.Emoji -> {
                    // 表情图片目前用文字替代，后续可以改为显示图片
                    append(node.text)
                }
            }
        }
    }
}
