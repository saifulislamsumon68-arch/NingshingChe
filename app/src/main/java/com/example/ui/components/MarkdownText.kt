package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Kalpurush

@Composable
fun MarkdownFormattedText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseTextColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 22.sp
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

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
                        text = parseInlineMarkdown(block.text, baseTextColor),
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
                        text = parseInlineMarkdown(block.text, baseTextColor),
                        fontFamily = Kalpurush,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = baseTextColor
                    )
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
                            text = parseInlineMarkdown(block.text, baseTextColor),
                            fontFamily = Kalpurush,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = baseTextColor,
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
                            text = parseInlineMarkdown(block.text, baseTextColor),
                            fontFamily = Kalpurush,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = baseTextColor,
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
                            text = parseInlineMarkdown(block.text, baseTextColor),
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
                Regex("""^(\*|-|•|\d+[.)])\s+""").containsMatchIn(nextTrimmed)
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
 * Parses inline markdown elements like **bold**, *italic*, `code` into an AnnotatedString.
 */
fun parseInlineMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            // Bold with ** or __
            if (cursor + 1 < length && (text.startsWith("**", cursor) || text.startsWith("__", cursor))) {
                val marker = text.substring(cursor, cursor + 2)
                val endIndex = text.indexOf(marker, cursor + 2)
                if (endIndex != -1) {
                    val boldContent = text.substring(cursor + 2, endIndex)
                    val start = length
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
