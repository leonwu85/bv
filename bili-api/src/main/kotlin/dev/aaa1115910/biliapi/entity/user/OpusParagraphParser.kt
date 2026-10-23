package dev.aaa1115910.biliapi.entity.user

import dev.aaa1115910.biliapi.http.entity.dynamic.OpusParagraph
import dev.aaa1115910.biliapi.http.entity.dynamic.OpusTextNode

object OpusParagraphParser {
    fun parse(paragraphs: List<OpusParagraph>, depth: Int = 0): List<ArticleParagraph> {
        if (depth >= 16) return emptyList()
        return paragraphs.mapNotNull { para ->
            when (para.paraType) {
                1 -> { // TEXT
                    val nodes = para.text?.nodes?.mapNotNull { node ->
                        parseOpusTextNode(node)
                    } ?: emptyList()
                    if (nodes.isNotEmpty()) {
                        ArticleParagraph.TextParagraph(nodes = nodes)
                    } else null
                }
                4 -> {
                    val children = if (para.blockquote != null) parse(para.blockquote.children, depth + 1)
                        else para.text?.nodes?.mapNotNull(::parseOpusTextNode)?.takeIf { it.isNotEmpty() }
                            ?.let { listOf(ArticleParagraph.TextParagraph(nodes = it)) }.orEmpty()
                    children.takeIf { it.isNotEmpty() }?.let { ArticleParagraph.QuoteParagraph(it) }
                }
                2 -> { // PICTURES
                    val pictures = para.pic?.pics?.map { pic ->
                        ArticlePicture(
                            url = pic.url ?: "",
                            width = pic.width ?: 0,
                            height = pic.height ?: 0
                        )
                    }?.filter { it.url.isNotBlank() } ?: emptyList()
                    if (pictures.isNotEmpty()) {
                        ArticleParagraph.PicturesParagraph(pictures = pictures)
                    } else null
                }
                3 -> { // LINE
                    val picture = para.line?.pic?.let { pic ->
                        ArticlePicture(
                            url = pic.url ?: "",
                            width = pic.width ?: 0,
                            height = pic.height ?: 0
                        )
                    }
                    ArticleParagraph.LineParagraph(picture = picture)
                }
                else -> null
            }
        }
    }

    /**
     * 解析 Opus 文本节点
     */
    private fun parseOpusTextNode(node: OpusTextNode): ArticleTextNode? {
        return when {
            node.emote != null -> ArticleTextNode(
                text = node.emote.text ?: node.rawText,
                type = TextNodeType.Emoji,
                emojiUrl = node.emote.iconUrl
            )
            node.rich != null -> ArticleTextNode(
                text = node.rich.text ?: node.rawText,
                type = TextNodeType.Link,
                linkUrl = node.rich.jumpUrl
            )
            node.word != null -> ArticleTextNode(
                text = node.word.words ?: node.rawText,
                type = TextNodeType.Plain,
                isBold = node.word.style?.bold ?: false,
                isItalic = node.word.style?.italic ?: false
            )
            else -> if (node.rawText.isNotBlank()) {
                ArticleTextNode(text = node.rawText, type = TextNodeType.Plain)
            } else null
        }
    }

}
