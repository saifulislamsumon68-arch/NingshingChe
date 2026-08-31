package com.example.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardRichTextEditor(
    content: String,
    onContentChange: (String) -> Unit,
    label: String = "প্রবন্ধের মূল বিষয়বস্তু (Rich Text Content)",
    modifier: Modifier = Modifier
) {
    var isPreviewMode by remember { mutableStateOf(false) }

    // Undo / Redo history tracking
    val history = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableStateOf(-1) }

    fun updateWithHistory(newText: String) {
        if (historyIndex >= 0 && historyIndex < history.size - 1) {
            while (history.size > historyIndex + 1) {
                history.removeAt(history.size - 1)
            }
        }
        history.add(newText)
        historyIndex = history.size - 1
        onContentChange(newText)
    }

    fun applyFormatting(prefix: String, suffix: String = "") {
        val updated = "$content\n$prefix $suffix"
        updateWithHistory(updated)
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            onContentChange(history[historyIndex])
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            onContentChange(history[historyIndex])
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Label & Mode Switch (Edit vs Live Preview)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !isPreviewMode,
                        onClick = { isPreviewMode = false },
                        label = { Text("এডিটর", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    FilterChip(
                        selected = isPreviewMode,
                        onClick = { isPreviewMode = true },
                        label = { Text("লাইভ প্রিভিউ", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rich Toolbar (When in Edit mode)
            AnimatedVisibility(visible = !isPreviewMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Undo / Redo
                        ToolbarIconButton(icon = Icons.Default.Undo, desc = "Undo") { undo() }
                        ToolbarIconButton(icon = Icons.Default.Redo, desc = "Redo") { redo() }

                        ToolbarDivider()

                        // Heading 1, 2, 3
                        ToolbarTextButton("H1") { applyFormatting("## ", "") }
                        ToolbarTextButton("H2") { applyFormatting("### ", "") }
                        ToolbarTextButton("H3") { applyFormatting("#### ", "") }
                        ToolbarTextButton("P") { applyFormatting("", "\n") }

                        ToolbarDivider()

                        // Formatting
                        ToolbarIconButton(icon = Icons.Default.FormatBold, desc = "Bold") {
                            applyFormatting("**বোল্ড টেক্সট**")
                        }
                        ToolbarIconButton(icon = Icons.Default.FormatItalic, desc = "Italic") {
                            applyFormatting("*ইটালিক টেক্সট*")
                        }
                        ToolbarIconButton(icon = Icons.Default.FormatUnderlined, desc = "Underline") {
                            applyFormatting("<u>আন্ডারলাইন</u>")
                        }
                        ToolbarIconButton(icon = Icons.Default.FormatQuote, desc = "Blockquote") {
                            applyFormatting("> উদ্ধৃতি বা কোটেশন টেক্সট")
                        }
                        ToolbarIconButton(icon = Icons.Default.Code, desc = "Code") {
                            applyFormatting("```\nকোড ব্লক\n```")
                        }

                        ToolbarDivider()

                        // Lists
                        ToolbarIconButton(icon = Icons.Default.FormatListBulleted, desc = "Bullet List") {
                            applyFormatting("• তালিকা আইটেম")
                        }
                        ToolbarIconButton(icon = Icons.Default.FormatListNumbered, desc = "Numbered List") {
                            applyFormatting("১. ক্রমানুসারিক আইটেম")
                        }
                        ToolbarIconButton(icon = Icons.Default.Link, desc = "Link") {
                            applyFormatting("[লিংকের নাম](https://ningshingche.com)")
                        }

                        ToolbarDivider()

                        // Alignment tags
                        ToolbarIconButton(icon = Icons.Default.FormatAlignLeft, desc = "Align Left") {
                            applyFormatting("<div align='left'>", "</div>")
                        }
                        ToolbarIconButton(icon = Icons.Default.FormatAlignCenter, desc = "Align Center") {
                            applyFormatting("<div align='center'>", "</div>")
                        }
                        ToolbarIconButton(icon = Icons.Default.FormatAlignRight, desc = "Align Right") {
                            applyFormatting("<div align='right'>", "</div>")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Rich Text Input or Live Preview
            if (!isPreviewMode) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { updateWithHistory(it) },
                    placeholder = {
                        Text(
                            "এখানে প্রবন্ধের বিষয়বস্তু লিখুন...\n(Markdown ও সাধারণ HTML ফরম্যাট সমর্থিত)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 400.dp)
                        .testTag("dashboard_rich_editor_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                // Live Public Style Preview Box
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 400.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "লাইভ প্রকাশনা প্রিভিউ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        if (content.isBlank()) {
                            Text(
                                text = "কোনো বিষয়বস্তু লিখা হয়নি। এডিটর মোডে ফিরে গিয়ে লেখা শুরু করুন।",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = FontStyle.Italic
                                )
                            )
                        } else {
                            // Render paragraphs and formatted blocks cleanly
                            val lines = content.split("\n")
                            lines.forEach { line ->
                                when {
                                    line.startsWith("###") -> {
                                        Text(
                                            text = line.removePrefix("###").trim(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                    line.startsWith("##") -> {
                                        Text(
                                            text = line.removePrefix("##").trim(),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Serif,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        )
                                    }
                                    line.startsWith(">") -> {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = line.removePrefix(">").trim(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontStyle = FontStyle.Italic,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                    line.startsWith("•") || line.startsWith("-") -> {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                            )
                                            Text(
                                                text = line.removePrefix("•").removePrefix("-").trim(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    line.isNotBlank() -> {
                                        Text(
                                            text = line.replace(Regex("<[^>]*>"), ""),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                lineHeight = 24.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun ToolbarTextButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(height = 26.dp, width = 32.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(18.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    )
}
