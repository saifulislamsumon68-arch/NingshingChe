package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.Kalpurush

private val YOUTUBE_RE = Regex(
    """https?://(?:www\.)?(?:youtube\.com/(?:watch\?v=|shorts/|embed/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})"""
)

/**
 * Renders markdown from the AI assistant with full rich-media support:
 *  - **bold**, *italic*, `code`, headings, lists, quotes and code blocks
 *  - `[links](url)` — drawn in the primary colour, underlined and tappable
 *  - `![alt](image-url)` — loads the image inline (Coil)
 *  - a bare YouTube URL on its own line — embeds the player (iframe) inline
 */
@Composable
fun MarkdownFormattedText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseTextColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 22.sp,
    onLinkClick: (String) -> Unit = {}
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val linkColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headFontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 16.sp
                        else -> 15.sp
                    }
                    Text(
                        text = parseInlineMarkdown(block.text, baseTextColor, linkColor, onLinkClick),
                        fontFamily = Kalpurush,
                        fontWeight = FontWeight.Bold,
                        fontSize = headFontSize,
                        lineHeight = (headFontSize.value + 6).sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text, baseTextColor, linkColor, onLinkClick),
                        fontFamily = Kalpurush,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = baseTextColor,
                        style = TextStyle(linkStyle = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ))
                    )
                }
                is MarkdownBlock.Image -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    ) {
                        PortalAsyncImage(
                            url = block.url,
                            contentDescription = block.alt,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        )
                    }
                }
                is MarkdownBlock.Video -> {
                    YouTubeEmbed(url = block.url)
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = parseInlineMarkdown(block.text, baseTextColor, linkColor, onLinkClick),
                            fontFamily = Kalpurush,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = baseTextColor,
                            style = TextStyle(linkStyle = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                )
                            )),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            fontFamily = Kalpurush,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = parseInlineMarkdown(block.text, baseTextColor, linkColor, onLinkClick),
                            fontFamily = Kalpurush,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = baseTextColor,
                            style = TextStyle(linkStyle = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                )
                            )),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = parseInlineMarkdown(block.text, baseTextColor, linkColor, onLinkClick),
                            fontFamily = Kalpurush,
                            fontStyle = FontStyle.Italic,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class BlockQuote(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class Image(val url: String, val alt: String) : MarkdownBlock
    data class Video(val url: String) : MarkdownBlock
    object Divider : MarkdownBlock
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = raw.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i].trimEnd()
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // Code block
        if (trimmed.startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip ending ```
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n")))
            continue
        }

        // Divider
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // Headings
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 4)
            val headText = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Heading(level, headText))
            i++
            continue
        }

        // BlockQuote
        if (trimmed.startsWith(">")) {
            val quoteText = trimmed.drop(1).trim()
            blocks.add(MarkdownBlock.BlockQuote(quoteText))
            i++
            continue
        }

        // Bullet lists (*, -, •)
        val bulletMatch = Regex("""^(\*|-|•)\s+(.*)""").find(trimmed)
        if (bulletMatch != null) {
            val itemText = bulletMatch.groupValues[2].trim()
            blocks.add(MarkdownBlock.BulletItem(itemText))
            i++
            continue
        }

        // Numbered lists (1. , 2. )
        val numberMatch = Regex("""^(\d+)[.)]\s+(.*)""").find(trimmed)
        if (numberMatch != null) {
            val num = numberMatch.groupValues[1]
            val itemText = numberMatch.groupValues[2].trim()
            blocks.add(MarkdownBlock.NumberedItem(num, itemText))
            i++
            continue
        }

        // Image ![alt](url) — block-level only.
        val imageMatch = Regex("""^!\[([^\]]*)\]\(([^)\s]+)\)$""").find(trimmed)
        if (imageMatch != null) {
            blocks.add(
                MarkdownBlock.Image(
                    url = imageMatch.groupValues[2].trim(),
                    alt = imageMatch.groupValues[1].trim().ifBlank { "ছবি" }
                )
            )
            i++
            continue
        }

        // Bare YouTube URL on its own line → inline iframe embed.
        if (YOUTUBE_RE.containsMatchIn(trimmed)) {
            blocks.add(MarkdownBlock.Video(trimmed))
            i++
            continue
        }

        // Regular Paragraph
        val paragraphLines = mutableListOf(line)
        i++
        while (i < lines.size) {
            val next = lines[i].trimEnd()
            val nextTrimmed = next.trim()
            if (nextTrimmed.isEmpty() ||
                nextTrimmed.startsWith("#") ||
                nextTrimmed.startsWith("```") ||
                nextTrimmed.startsWith(">") ||
                nextTrimmed == "---" ||
                Regex("""^(\*|-|•|\d+[.)])\s+""").containsMatchIn(nextTrimmed) ||
                YOUTUBE_RE.containsMatchIn(nextTrimmed) ||
                Regex("""^!\[[^\]]*\]\([^)\s]+\)$""").containsMatchIn(nextTrimmed)
            ) {
                break
            }
            paragraphLines.add(next)
            i++
        }
        blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
    }

    return blocks
}

/**
 * Parses inline markdown: **bold**, *italic*, `code`, `[link](url)` and bare http(s)
 * URLs into an [AnnotatedString]. Links are tappable via [onLinkClick].
 */
fun parseInlineMarkdown(
    text: String,
    defaultColor: Color,
    linkColor: Color = Color(0xFF0D6EFD),
    onLinkClick: (String) -> Unit = {}
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            // Image reference ![alt](url) — keep alt text only.
            if (text.startsWith("![", cursor)) {
                val closeParen = text.indexOf(')', cursor)
                val closeBracket = text.indexOf(']', cursor)
                if (closeBracket != -1 && closeParen != -1 && closeBracket < closeParen) {
                    val alt = text.substring(cursor + 2, closeBracket)
                    append(alt)
                    cursor = closeParen + 1
                    continue
                }
            }

            // Link [label](url)
            if (text[cursor] == '[') {
                val closeBracket = text.indexOf(']', cursor + 1)
                if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 1)
                    if (closeParen != -1) {
                        val label = text.substring(cursor + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen).trim()
                        pushLink(
                            LinkAnnotation.Clickable(tag = url, linkInteractionListener = { _ -> onLinkClick(url) })
                        )
                        pushStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        append(if (label.isBlank()) url else label)
                        pop()
                        pop()
                        cursor = closeParen + 1
                        continue
                    }
                }
            }

            // Bare URL auto-link
            if (text.startsWith("https://", cursor, ignoreCase = true) ||
                text.startsWith("https://", cursor, ignoreCase = true)
            ) {
                var end = cursor
                while (end < length && !text[end].isWhitespace() && text[end] != '।' && text[end] != ',' &&
                    !(end > cursor && text[end] == ')')
                ) {
                    end++
                }
                val url = text.substring(cursor, end)
                pushLink(
                    LinkAnnotation.Clickable(tag = url, linkInteractionListener = { _ -> onLinkClick(url) })
                )
                pushStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.underline,
                        fontWeight = FontWeight.Medium
                    )
                )
                append(url)
                pop()
                pop()
                cursor = end
                continue
            }

            // Bold with ** or __
            if (cursor + 1 < length && (text.startsWith("**", cursor) || text.startsWith("__", cursor))) {
                val marker = text.substring(cursor, cursor + 2)
                val endIndex = text.indexOf(marker, cursor + 2)
                if (endIndex != -1) {
                    val boldContent = text.substring(cursor + 2, endIndex)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(boldContent)
                    pop()
                    cursor = endIndex + 2
                    continue
                }
            }

            // Inline code with `
            if (text[cursor] == '`') {
                val endIndex = text.indexOf('`', cursor + 1)
                if (endIndex != -1) {
                    val codeContent = text.substring(cursor + 1, endIndex)
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium))
                    append(codeContent)
                    pop()
                    cursor = endIndex + 1
                    continue
                }
            }

            // Italic with * or _
            if (text[cursor] == '*' || text[cursor] == '_') {
                val marker = text[cursor]
                val endIndex = text.indexOf(marker, cursor + 1)
                if (endIndex != -1 && endIndex > cursor + 1) {
                    val italicContent = text.substring(cursor + 1, endIndex)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicContent)
                    pop()
                    cursor = endIndex + 1
                    continue
                }
            }

            // Normal character
            append(text[cursor])
            cursor++
        }
    }
}

/** Inline YouTube player — a WebView hosting the official iframe embed. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbed(
    url: String,
    modifier: Modifier = Modifier
) {
    val videoId = YOUTUBE_RE.find(url)?.groupValues?.getOrNull(1) ?: return
    val embedSrc = "https://www.youtube.com/embed/$videoId?rel=0&playsinline=1&modestbranding=1&fs=1"
    val html = remember(embedSrc) {
        """"
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="view port" content="width=device-width, initial-scale=1.0, user-scalable=no">
        <style>
          html, body { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
          #player { position:absolute; top:0; left:0; width:100%; height:100%; border:0; }
        </style>
        </head>
        <body>
        <iframe id="player"
            src="$embedSrc"
            frameborder="0"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
            allowfullscreen></iframe>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornershape(12.dp))
            .background(AndroidColor.BLACK)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.BLACK)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        userAgentString = PLAYER_UA
                    }
                    webViewClient = WebViewCient()
                    webChromeClient = WebChomeCient()
                    loadDataWithBaseURL("https://www.youtube.com/", html, "text/html", "utf-8", null)
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            onRelease = { view ->
                view.stopLoading()
                view.webChomeCient = null
                view.destroy()
            }
        )
    }
}

private const val PLAYER_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"