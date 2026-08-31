package com.example.ui.reader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.ui.editorial.Hairline
import com.example.ui.editorial.LocalEditorialTokens
import com.example.ui.editorial.ShimmerPlaceholder
import com.example.ui.theme.Kalpurush
import java.util.regex.Pattern

/**
 * Semantic block model representing parsed WYSIWYG HTML content.
 */
sealed interface HtmlBlock {
    data class Paragraph(val text: String) : HtmlBlock
    data class Heading(val level: Int, val text: String) : HtmlBlock
    data class Blockquote(val text: String) : HtmlBlock
    data class ListItem(val number: Int?, val text: String) : HtmlBlock
    data class Image(val url: String, val alt: String = "") : HtmlBlock
    data class CodeBlock(val code: String) : HtmlBlock
    data object Divider : HtmlBlock
}

/**
 * Parses WYSIWYG HTML into structured blocks.
 */
object HtmlArticleParser {

    private val BLOCK_REGEX = Pattern.compile(
        "(?is)<(h[1-6]|p|blockquote|ul|ol|li|pre|img|hr)[^>]*>(.*?)(?:</\\1>|$)",
        Pattern.DOTALL
    )
    private val IMG_REGEX = Pattern.compile(
        "(?is)<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>",
        Pattern.DOTALL
    )
    private val ALT_REGEX = Pattern.compile(
        "(?is)alt=[\"']([^\"']*)[\"']",
        Pattern.DOTALL
    )

    fun parse(html: String): List<HtmlBlock> {
        if (html.isBlank()) return emptyList()

        val blocks = mutableListOf<HtmlBlock>()
        val cleanHtml = html
            .replace(Regex("(?is)<(script|style).*?</\\1>"), "")
            .trim()

        // Extract image blocks that might be stand-alone or inside tags
        val matcher = BLOCK_REGEX.matcher(cleanHtml)
        var lastEnd = 0

        while (matcher.find()) {
            val tag = matcher.group(1)?.lowercase() ?: ""
            val fullMatch = matcher.group(0) ?: ""
            val inner = matcher.group(2) ?: ""

            // Check if inner content contains an image
            val imgMatch = IMG_REGEX.matcher(fullMatch)
            if (tag == "img" || imgMatch.find()) {
                val imgSrc = if (tag == "img") {
                    val m = Pattern.compile("src=[\"']([^\"']+)[\"']").matcher(fullMatch)
                    if (m.find()) m.group(1) else null
                } else {
                    imgMatch.group(1)
                }
                if (!imgSrc.isNullOrBlank()) {
                    val altM = ALT_REGEX.matcher(fullMatch)
                    val alt = if (altM.find()) altM.group(1).orEmpty() else ""
                    blocks.add(HtmlBlock.Image(url = imgSrc.trim(), alt = alt))
                }
                // Also parse text surrounding image if any
                val textOnly = fullMatch.replace(Regex("(?is)<img[^>]*>"), "").trim()
                if (textOnly.isNotBlank() && cleanInlineHtml(textOnly).isNotBlank()) {
                    blocks.add(HtmlBlock.Paragraph(textOnly))
                }
                continue
            }

            when {
                tag.startsWith("h") -> {
                    val level = tag.substring(1).toIntOrNull() ?: 2
                    val text = cleanInlineHtml(inner)
                    if (text.isNotBlank()) {
                        blocks.add(HtmlBlock.Heading(level = level, text = inner.trim()))
                    }
                }
                tag == "blockquote" -> {
                    val text = cleanInlineHtml(inner)
                    if (text.isNotBlank()) {
                        blocks.add(HtmlBlock.Blockquote(text = inner.trim()))
                    }
                }
                tag == "li" -> {
                    val text = cleanInlineHtml(inner)
                    if (text.isNotBlank()) {
                        blocks.add(HtmlBlock.ListItem(number = null, text = inner.trim()))
                    }
                }
                tag == "pre" -> {
                    val text = cleanInlineHtml(inner)
                    if (text.isNotBlank()) {
                        blocks.add(HtmlBlock.CodeBlock(code = text))
                    }
                }
                tag == "hr" -> {
                    blocks.add(HtmlBlock.Divider)
                }
                tag == "p" || tag == "ul" || tag == "ol" -> {
                    // Check for nested li
                    if (inner.contains("<li", ignoreCase = true)) {
                        val liMatcher = Pattern.compile("(?is)<li[^>]*>(.*?)</li>").matcher(inner)
                        var index = 1
                        val isOrdered = tag == "ol"
                        while (liMatcher.find()) {
                            val liInner = liMatcher.group(1).orEmpty()
                            if (cleanInlineHtml(liInner).isNotBlank()) {
                                blocks.add(HtmlBlock.ListItem(number = if (isOrdered) index++ else null, text = liInner.trim()))
                            }
                        }
                    } else {
                        val text = cleanInlineHtml(inner)
                        if (text.isNotBlank()) {
                            blocks.add(HtmlBlock.Paragraph(text = inner.trim()))
                        }
                    }
                }
            }
            lastEnd = matcher.end()
        }

        // Fallback: If no structured tags matched (e.g. plain text or raw paragraphs), split by double newlines
        if (blocks.isEmpty()) {
            val paragraphs = cleanHtml.split(Regex("\n\n+"))
            for (p in paragraphs) {
                val trimmed = p.trim()
                if (trimmed.isNotBlank()) {
                    blocks.add(HtmlBlock.Paragraph(trimmed))
                }
            }
        }

        return blocks
    }

    fun cleanInlineHtml(html: String): String = html
        .replace(Regex("(?s)<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
}

/**
 * Builds an AnnotatedString from HTML inline tags (bold, italic, links, etc.).
 */
fun buildHtmlAnnotatedString(
    html: String,
    accentColor: Color,
    textColor: Color,
    fontFamily: FontFamily = Kalpurush
): Pair<AnnotatedString, List<Pair<IntRange, String>>> {
    val links = mutableListOf<Pair<IntRange, String>>()
    val builder = AnnotatedString.Builder()

    val linkPattern = Pattern.compile("(?is)<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>")
    val formatPattern = Pattern.compile("(?is)<(b|strong|i|em|u|s|strike|code|br\\s*/?)[^>]*>(.*?)</\\1>|<br\\s*/?>")

    // Normalize entities and linebreaks
    val workingHtml = html
        .replace(Regex("(?is)<br\\s*/?>"), "\n")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    // Parse links and formatting
    var cursor = 0
    val linkMatcher = linkPattern.matcher(workingHtml)

    fun appendFormattedText(chunk: String) {
        var innerCursor = 0
        val fmtMatcher = formatPattern.matcher(chunk)

        while (fmtMatcher.find()) {
            val before = chunk.substring(innerCursor, fmtMatcher.start())
            val cleanBefore = before.replace(Regex("<[^>]+>"), "")
            builder.append(cleanBefore)

            val tag = fmtMatcher.group(1)?.lowercase().orEmpty()
            val inner = fmtMatcher.group(2).orEmpty().replace(Regex("<[^>]+>"), "")

            val startPos = builder.length
            builder.append(inner)
            val endPos = builder.length

            if (startPos < endPos) {
                when (tag) {
                    "b", "strong" -> builder.addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        startPos,
                        endPos
                    )
                    "i", "em" -> builder.addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        startPos,
                        endPos
                    )
                    "u" -> builder.addStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        startPos,
                        endPos
                    )
                    "s", "strike" -> builder.addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        startPos,
                        endPos
                    )
                    "code" -> builder.addStyle(
                        SpanStyle(
                            background = accentColor.copy(alpha = 0.12f),
                            fontWeight = FontWeight.Medium
                        ),
                        startPos,
                        endPos
                    )
                }
            }
            innerCursor = fmtMatcher.end()
        }

        if (innerCursor < chunk.length) {
            val remaining = chunk.substring(innerCursor).replace(Regex("<[^>]+>"), "")
            builder.append(remaining)
        }
    }

    while (linkMatcher.find()) {
        val beforeLink = workingHtml.substring(cursor, linkMatcher.start())
        appendFormattedText(beforeLink)

        val href = linkMatcher.group(1).orEmpty()
        val linkText = linkMatcher.group(2).orEmpty().replace(Regex("<[^>]+>"), "")

        val linkStart = builder.length
        builder.append(linkText)
        val linkEnd = builder.length

        if (linkStart < linkEnd) {
            val linkStyle = SpanStyle(
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
            builder.addStyle(
                linkStyle,
                linkStart,
                linkEnd
            )
            builder.addStringAnnotation(
                tag = "URL",
                annotation = href,
                start = linkStart,
                end = linkEnd
            )
            val linkAnnotation = LinkAnnotation.Url(
                url = href,
                styles = TextLinkStyles(style = linkStyle)
            )
            builder.addLink(linkAnnotation, linkStart, linkEnd)
            links.add(IntRange(linkStart, linkEnd) to href)
        }

        cursor = linkMatcher.end()
    }

    if (cursor < workingHtml.length) {
        val remaining = workingHtml.substring(cursor)
        appendFormattedText(remaining)
    }

    return builder.toAnnotatedString() to links
}

/**
 * Triggers a subtle tactile vibration for long-press on links/images.
 */
fun triggerTactileVibration(context: android.content.Context) {
    try {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        }
    } catch (_: Exception) { }
}

/**
 * Rich WYSIWYG HTML Article Body Renderer with 100% width images,
 * interactive zoom modal dialog, clickable styled links, blockquotes, headings, and lists.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RichHtmlArticleBody(
    html: String,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val tokens = LocalEditorialTokens.current

    var enlargedImageUrl by remember { mutableStateOf<String?>(null) }
    val blocks = remember(html) { HtmlArticleParser.parse(html) }

    if (blocks.isEmpty()) {
        Text(
            text = "এই প্রবন্ধে কোনো বিষয়বস্তু পাওয়া যায়নি।",
            fontFamily = Kalpurush,
            fontSize = fontSizeSp.sp,
            color = tokens.inkMuted,
            modifier = modifier.padding(vertical = 16.dp)
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is HtmlBlock.Heading -> {
                    val headingScale = when (block.level) {
                        1 -> 1.45f
                        2 -> 1.30f
                        3 -> 1.18f
                        else -> 1.10f
                    }
                    val (annotated, _) = buildHtmlAnnotatedString(
                        html = block.text,
                        accentColor = tokens.accent,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = annotated,
                        fontFamily = Kalpurush,
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSizeSp * headingScale).sp,
                        lineHeight = ((fontSizeSp * headingScale) * 1.35f).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                is HtmlBlock.Blockquote -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = tokens.accentSoft.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, tokens.accent.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left accent quotation bar
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .background(tokens.accent)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = tokens.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                val (annotated, _) = buildHtmlAnnotatedString(
                                    html = block.text,
                                    accentColor = tokens.accent,
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = annotated,
                                    fontFamily = Kalpurush,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = (fontSizeSp * 1.02f).sp,
                                    lineHeight = ((fontSizeSp * 1.02f) * lineSpacingMultiplier).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                is HtmlBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val bulletText = if (block.number != null) "${block.number}." else "•"
                        Text(
                            text = bulletText,
                            fontFamily = Kalpurush,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeSp.sp,
                            color = tokens.accent,
                            modifier = Modifier.width(24.dp)
                        )
                        val (annotated, links) = buildHtmlAnnotatedString(
                            html = block.text,
                            accentColor = tokens.accent,
                            textColor = MaterialTheme.colorScheme.onSurface
                        )
                        ClickableHtmlText(
                            annotatedString = annotated,
                            fontSizeSp = fontSizeSp,
                            lineSpacingMultiplier = lineSpacingMultiplier,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is HtmlBlock.Image -> {
                    // Images initially have 100% width and open enlargement modal on click
                    FullWidthArticleImage(
                        url = block.url,
                        alt = block.alt,
                        onClick = { enlargedImageUrl = block.url },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            triggerTactileVibration(context)
                        }
                    )
                }

                is HtmlBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tokens.surfaceSunken,
                        border = BorderStroke(1.dp, tokens.rule),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = block.code,
                            fontSize = (fontSizeSp * 0.9f).sp,
                            lineHeight = ((fontSizeSp * 0.9f) * 1.4f).sp,
                            color = tokens.inkSoft,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                is HtmlBlock.Divider -> {
                    Hairline(modifier = Modifier.padding(vertical = 8.dp))
                }

                is HtmlBlock.Paragraph -> {
                    val (annotated, _) = buildHtmlAnnotatedString(
                        html = block.text,
                        accentColor = tokens.accent,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                    ClickableHtmlText(
                        annotatedString = annotated,
                        fontSizeSp = fontSizeSp,
                        lineSpacingMultiplier = lineSpacingMultiplier,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Modal dialog to enlarge image when tapped
    enlargedImageUrl?.let { imageUrl ->
        ImageEnlargeModal(
            imageUrl = imageUrl,
            onDismiss = { enlargedImageUrl = null }
        )
    }
}

/**
 * 100% width responsive image with smooth loading indicator and tap to enlarge overlay badge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullWidthArticleImage(
    url: String,
    alt: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalEditorialTokens.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, tokens.rule.copy(alpha = 0.5f)),
        color = tokens.surfaceSunken,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = alt.ifBlank { "প্রবন্ধের ছবি" },
                contentScale = ContentScale.FillWidth,
                loading = {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(tokens.surfaceSunken),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ছবি লোড করা যায়নি",
                            fontFamily = Kalpurush,
                            fontSize = 13.sp,
                            color = tokens.inkMuted
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            // Zoom affordance badge in bottom right corner
            Surface(
                shape = RoundedCornerShape(topStart = 10.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.padding(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "বড় করে দেখুন",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "জুম",
                        fontFamily = Kalpurush,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * HTML Text component supporting rich text and links with fluid vertical scrolling.
 */
@Composable
private fun ClickableHtmlText(
    annotatedString: AnnotatedString,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    modifier: Modifier = Modifier
) {
    Text(
        text = annotatedString,
        fontFamily = Kalpurush,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/**
 * Fullscreen Interactive Image Enlargement Modal Dialog with Pinch-to-Zoom & Pan.
 */
@Composable
fun ImageEnlargeModal(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
        ) {
            // Interactive Zoomable Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "পূর্ণ আকারের ছবি",
                    contentScale = ContentScale.Fit,
                    loading = {
                        ShimmerPlaceholder(modifier = Modifier.size(60.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }

            // Top action buttons bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Open in browser
                    IconButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl)))
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "ব্রাউজারে খুলুন",
                            tint = Color.White
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, imageUrl)
                            }
                            context.startActivity(Intent.createChooser(send, "ছবি শেয়ার করুন"))
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "শেয়ার",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom zoom hint banner
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (scale > 1.05f) "জুম রিসেট করতে ট্যাপ করুন" else "জুম করতে দুই আঙুল ব্যবহার করুন",
                    fontFamily = Kalpurush,
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .clickable {
                            scale = 1f
                            offset = Offset.Zero
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
    }
}
