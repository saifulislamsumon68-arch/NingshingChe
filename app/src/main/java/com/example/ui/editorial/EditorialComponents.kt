package com.example.ui.editorial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.components.PortalAsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.portal.ArticleSummary
import com.example.data.portal.AuthorRef
import com.example.data.portal.CategoryRef
import com.example.data.portal.GalleryItem
import com.example.data.portal.PdfBook
import com.example.data.portal.VideoItem
import com.example.data.portal.excerptOf
import com.example.data.portal.stripHtml
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Reusable building blocks for the modern-editorial reader.
 *
 * Conventions
 * -----------
 * - Nothing here talks to a ViewModel or a repository; every composable takes
 *   plain data plus lambdas, so all of it renders in a preview.
 * - Images are always loaded through [EditorialImage], which owns the loading
 *   and error affordances. A missing image collapses to a tinted rule rather
 *   than an empty grey box.
 * - Cards are flat ([CardDefaults.cardElevation] stays at 0). Hierarchy comes
 *   from type scale, hairlines and whitespace.
 */

// ---------------------------------------------------------------------------
// Primitives
// ---------------------------------------------------------------------------

/** Full-width hairline, the editorial equivalent of a divider. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = LocalEditorialTokens.current.rule
) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalEditorialTokens.current.accent
) {
    Text(
        text = text,
        style = EditorialType.Eyebrow,
        color = color,
        modifier = modifier
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EditorialSpace.gutter)
            .padding(top = EditorialSpace.lg, bottom = EditorialSpace.sm),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = EditorialType.Headline,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = EditorialType.Caption,
                    color = LocalEditorialTokens.current.inkMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, style = EditorialType.Subtitle, color = LocalEditorialTokens.current.accent)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = LocalEditorialTokens.current.accent
                )
            }
        }
    }
}

/** Coil image with a graceful placeholder and direct loading. */
@Composable
fun EditorialImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(EditorialShape.thumb)
) {
    PortalAsyncImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale
    )
}

@Composable
private fun EmptyThumb(modifier: Modifier = Modifier, shape: Shape) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(LocalEditorialTokens.current.surfaceSunken)
            .border(1.dp, LocalEditorialTokens.current.rule, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Image,
            contentDescription = null,
            tint = LocalEditorialTokens.current.inkMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-progress"
    )
    val base = LocalEditorialTokens.current.surfaceSunken
    val highlight = base.copy(alpha = 0.55f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(0f, 0f),
        end = Offset(1000f * progress.coerceAtLeast(0.001f), 1000f * progress.coerceAtLeast(0.001f))
    )
}

// ---------------------------------------------------------------------------
// Meta / byline
// ---------------------------------------------------------------------------

/**
 * Formats `2025-06-17` as `১৭ জুন, ২০২৫`.
 *
 * Uses `SimpleDateFormat` rather than `java.time` on purpose: `minSdk` is 24 and
 * this module does not enable core library desugaring, so `java.time.*` would
 * throw `NoClassDefFoundError` on Android 7.
 */
fun formatBengaliDate(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.take(10))
            ?: return iso.take(10)
        SimpleDateFormat("d MMMM, yyyy", Locale("bn", "BD")).format(parsed)
    }.getOrDefault(iso.take(10))
}

@Composable
fun Byline(
    authorName: String,
    publishedDate: String,
    readingTimeMinutes: Int,
    modifier: Modifier = Modifier,
    category: String? = null
) {
    val parts = buildList {
        if (authorName.isNotBlank()) add(authorName)
        if (!category.isNullOrBlank()) add(category)
        val date = formatBengaliDate(publishedDate)
        if (date.isNotBlank()) add(date)
        if (readingTimeMinutes > 0) add("$readingTimeMinutes মিনিট")
    }
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString("  ·  "),
        style = EditorialType.Caption,
        color = LocalEditorialTokens.current.inkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun CategoryPill(
    title: String,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false
) {
    val tokens = LocalEditorialTokens.current
    val background = if (selected) tokens.accent else tokens.accentSoft.copy(alpha = 0.6f)
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else tokens.accent
    val border = if (selected) Color.Transparent else tokens.accent.copy(alpha = 0.35f)
    Surface(
        shape = RoundedCornerShape(EditorialShape.chip),
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = title,
            style = EditorialType.Caption,
            fontFamily = com.example.ui.theme.Kalpurush,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Article cards
// ---------------------------------------------------------------------------

/** Full-bleed hero panel with a scrim and bottom-aligned headline. */
@Composable
fun HeroArticleCard(
    article: ArticleSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalEditorialTokens.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tokens.surfaceSunken),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, tokens.rule.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
        ) {
            PortalAsyncImage(
                url = article.imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Multi-stop high contrast gradient overlay for pristine Bengali legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x33000000),
                                Color(0x11000000),
                                Color(0x99100C09),
                                Color(0xF5100C09)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (article.categoryTitle.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tokens.accent.copy(alpha = 0.92f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = article.categoryTitle,
                            style = EditorialType.Eyebrow.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = article.title,
                    style = EditorialType.Headline.copy(
                        fontSize = 19.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (article.authorName.isNotBlank()) {
                        Text(
                            text = article.authorName,
                            style = EditorialType.Caption.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    if (article.publishedDate.isNotBlank()) {
                        Text(
                            text = "•  ${article.publishedDate}",
                            style = EditorialType.Caption,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    if (article.readingTimeMinutes > 0) {
                        Text(
                            text = "•  ${article.readingTimeMinutes} মি.",
                            style = EditorialType.Caption,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

/** Landscape card used in horizontal rails: thumbnail left, text right. */
@Composable
fun RailArticleCard(
    article: ArticleSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(EditorialShape.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.width(268.dp)
    ) {
        Column {
            EditorialImage(
                url = article.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(EditorialShape.card))
            )
            Column(Modifier.padding(EditorialSpace.md)) {
                Text(
                    text = article.title,
                    style = EditorialType.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Byline(
                    authorName = article.authorName,
                    publishedDate = article.publishedDate,
                    readingTimeMinutes = article.readingTimeMinutes
                )
            }
        }
    }
}

/** The workhorse list row: text on the left, thumbnail on the right. */
@Composable
fun ArticleRow(
    article: ArticleSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.md),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (article.categoryTitle.isNotBlank()) {
                Eyebrow(article.categoryTitle)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = article.title,
                style = EditorialType.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (article.subTitle.isNotBlank()) {
                Text(
                    text = article.subTitle,
                    style = EditorialType.BodySmall,
                    color = LocalEditorialTokens.current.inkSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Byline(
                authorName = article.authorName,
                publishedDate = article.publishedDate,
                readingTimeMinutes = article.readingTimeMinutes
            )
        }
        Spacer(Modifier.width(EditorialSpace.md))
        EditorialImage(
            url = article.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 96.dp, height = 72.dp)
        )
    }
}

/** Numbered card for "special articles" — the numeral is the accent. */
@Composable
fun NumberedArticleCard(
    index: Int,
    article: ArticleSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.sm),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = String.format(Locale("bn", "BD"), "%d", index),
            style = EditorialType.Display,
            color = LocalEditorialTokens.current.accent.copy(alpha = 0.35f),
            modifier = Modifier.width(46.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                style = EditorialType.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Byline(
                authorName = article.authorName,
                publishedDate = article.publishedDate,
                readingTimeMinutes = article.readingTimeMinutes
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Rails
// ---------------------------------------------------------------------------

@Composable
fun AnimatedHamburgerIcon(
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.2.dp.toPx()
        val cap = StrokeCap.Round
        val w = size.width
        val h = size.height

        // 3 refined editorial lines
        drawLine(
            color = tint,
            start = Offset(2.dp.toPx(), h * 0.22f),
            end = Offset(w - 2.dp.toPx(), h * 0.22f),
            strokeWidth = stroke,
            cap = cap
        )
        drawLine(
            color = tint,
            start = Offset(2.dp.toPx(), h * 0.50f),
            end = Offset(w - 7.dp.toPx(), h * 0.50f),
            strokeWidth = stroke,
            cap = cap
        )
        drawLine(
            color = tint,
            start = Offset(2.dp.toPx(), h * 0.78f),
            end = Offset(w - 2.dp.toPx(), h * 0.78f),
            strokeWidth = stroke,
            cap = cap
        )
    }
}

@Composable
fun AiAssistantHomeBanner(
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalEditorialTokens.current
    Card(
        onClick = onAiClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tokens.isDark) Color(0xFF261814) else Color(0xFFFBF4EC)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.xs)
            .border(
                1.dp,
                if (tokens.isDark) Color(0xFF4A2F25) else Color(0xFFE8D3C1),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = tokens.accent,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "নিংশিং চে AI সহকারী",
                        fontFamily = com.example.ui.theme.Kalpurush,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "বিষ্ণুপ্রিয়া মণিপুরি ভাষা, সাহিত্য, সংস্কৃতি ও ইতিহাসের যে কোনো প্রশ্ন করুন",
                        fontFamily = com.example.ui.theme.Kalpurush,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = tokens.inkSoft
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick suggestion chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ভাষা আন্দোলন", "ইমচৌঘর", "মিংকৌ প্রথা", "বিশু উৎসব").forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (tokens.isDark) Color(0xFF38231C) else Color(0xFFFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (tokens.isDark) Color(0xFF5A392F) else Color(0xFFE2CEBC)
                        )
                    ) {
                        Text(
                            text = tag,
                            fontFamily = com.example.ui.theme.Kalpurush,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI সহকারীকে জিজ্ঞাসা করুন",
                    fontFamily = com.example.ui.theme.Kalpurush,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun getFallbackCategoryImage(slug: String, title: String): String {
    return when {
        slug.contains("history") || title.contains("ইতিহাস") ->
            "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&auto=format&fit=crop&q=80"
        slug.contains("literature") || title.contains("সাহিত্য") ->
            "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600&auto=format&fit=crop&q=80"
        slug.contains("culture") || title.contains("সংস্কৃতি") ->
            "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80"
        slug.contains("social") || title.contains("সমাজ") ->
            "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=600&auto=format&fit=crop&q=80"
        slug.contains("play") || title.contains("নাটক") ->
            "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=600&auto=format&fit=crop&q=80"
        else ->
            "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=600&auto=format&fit=crop&q=80"
    }
}

@Composable
fun CategoryVisualCard(
    category: CategoryRef,
    imageUrl: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LocalEditorialTokens.current.surfaceSunken),
        modifier = modifier
            .width(152.dp)
            .height(100.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, LocalEditorialTokens.current.accent, RoundedCornerShape(14.dp))
                else Modifier.border(1.dp, LocalEditorialTokens.current.rule, RoundedCornerShape(14.dp))
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PortalAsyncImage(
                url = imageUrl,
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // High contrast gradient overlay for crisp legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x33000000),
                                Color(0x881A120B),
                                Color(0xFA140D08)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category.title,
                    fontFamily = com.example.ui.theme.Kalpurush,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (category.subTitle.isNotBlank()) {
                    Text(
                        text = category.subTitle,
                        fontFamily = com.example.ui.theme.Kalpurush,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFFFFD59E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryRail(
    categories: List<CategoryRef>,
    articles: List<ArticleSummary> = emptyList(),
    selectedSlug: String? = null,
    onSelect: (CategoryRef) -> Unit
) {
    if (categories.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "বিষয় ও বিভাগ",
            subtitle = "বিষ্ণুপ্রিয়া মণিপুরি সাহিত্য ও সাংস্কৃতিক ধারা"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(categories, key = { it.id }) { category ->
                val firstArticle = articles.firstOrNull {
                    it.categorySlug == category.slug ||
                    it.categoryId == category.id ||
                    it.categoryTitle.equals(category.title, ignoreCase = true)
                }
                val thumbUrl = firstArticle?.imageUrl?.takeIf { it.isNotBlank() }
                    ?: getFallbackCategoryImage(category.slug, category.title)

                CategoryVisualCard(
                    category = category,
                    imageUrl = thumbUrl,
                    isSelected = category.slug == selectedSlug,
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

@Composable
fun GalleryModalDialog(
    items: List<GalleryItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onShare: (GalleryItem) -> Unit
) {
    if (items.isEmpty()) return

    val startPage = initialIndex.coerceIn(0, items.lastIndex)
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { items.size })
    val scope = rememberCoroutineScope()
    val tokens = LocalEditorialTokens.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(EditorialShape.sheet),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f)
                .padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // --- Header: counter, category, close -----------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(EditorialShape.chip),
                            color = tokens.accentSoft
                        ) {
                            Text(
                                text = items[pagerState.currentPage].category.ifBlank { "ছবি ঘর" },
                                fontFamily = com.example.ui.theme.Kalpurush,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (items.size > 1) {
                            Text(
                                text = "${bengaliDigits(pagerState.currentPage + 1)} / ${bengaliDigits(items.size)}",
                                fontFamily = com.example.ui.theme.Kalpurush,
                                fontSize = 12.sp,
                                color = tokens.inkMuted,
                                modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // --- Swipeable image ---------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.surfaceSunken)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val item = items[page]
                        PortalAsyncImage(
                            url = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                        )
                    }

                    // Arrow affordances, so the viewer is not swipe-only.
                    if (items.size > 1) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage - 1).coerceAtLeast(0)
                                    )
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "আগের ছবি",
                                tint = if (pagerState.currentPage > 0) Color.White
                                else Color.White.copy(alpha = 0.25f)
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage + 1).coerceAtMost(items.lastIndex)
                                    )
                                }
                            },
                            enabled = pagerState.currentPage < items.lastIndex,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "পরের ছবি",
                                tint = if (pagerState.currentPage < items.lastIndex) Color.White
                                else Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }

                // --- Caption ------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = items[pagerState.currentPage].title,
                        fontFamily = com.example.ui.theme.Kalpurush,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 25.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val description = items[pagerState.currentPage].description
                    if (description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = description,
                            fontFamily = com.example.ui.theme.Kalpurush,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = tokens.inkSoft,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Dot indicator
                    if (items.size > 1) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            items.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage) tokens.accent
                                            else tokens.ruleStrong
                                        )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onShare(items[pagerState.currentPage]) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = tokens.accent
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "শেয়ার করুন",
                                fontFamily = com.example.ui.theme.Kalpurush,
                                fontWeight = FontWeight.Bold,
                                color = tokens.accent
                            )
                        }
                    }
                }
            }
        }
    }
}

/** `12` -> `১২`, so the page counter matches the Bengali UI. */
private fun bengaliDigits(value: Int): String {
    val bengali = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return value.toString().map { ch ->
        if (ch in '0'..'9') bengali[ch - '0'] else ch
    }.joinToString("")
}

@Composable
fun ArticleRail(
    title: String,
    articles: List<ArticleSummary>,
    onArticleClick: (ArticleSummary) -> Unit,
    onSeeAll: (() -> Unit)? = null
) {
    if (articles.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            actionLabel = if (onSeeAll != null) "সব" else null,
            onAction = onSeeAll
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(articles, key = { it.id }) { article ->
                RailArticleCard(article = article, onClick = { onArticleClick(article) })
            }
        }
    }
}

@Composable
fun AuthorRail(
    authors: List<AuthorRef>,
    onAuthorClick: (AuthorRef) -> Unit
) {
    if (authors.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "লেখক", subtitle = "নিংশিং চে-এর নিয়মিত কলম")
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(authors, key = { it.id }) { author ->
                AuthorChip(author = author, onClick = { onAuthorClick(author) })
            }
        }
    }
}

@Composable
fun AuthorChip(author: AuthorRef, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(EditorialShape.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(168.dp)
    ) {
        Column(
            modifier = Modifier.padding(EditorialSpace.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                EditorialImage(
                    url = author.imageUrl,
                    contentDescription = author.name,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape
                )
            }
            Spacer(Modifier.height(EditorialSpace.sm))
            Text(
                text = author.name,
                style = EditorialType.Subtitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (author.designation.isNotBlank()) {
                Text(
                    text = author.designation,
                    style = EditorialType.Caption,
                    color = LocalEditorialTokens.current.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun GalleryGrid(
    items: List<GalleryItem>,
    onItemClick: (GalleryItem) -> Unit
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "ছবি ঘর", subtitle = "ইতিহাস ও সংস্কৃতির দৃশ্যপট")
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.sm)
        ) {
            items(items, key = { it.id }) { item ->
                Card(
                    onClick = { onItemClick(item) },
                    shape = RoundedCornerShape(EditorialShape.card),
                    colors = CardDefaults.cardColors(containerColor = LocalEditorialTokens.current.surfaceSunken),
                    modifier = Modifier.width(180.dp)
                ) {
                    Box {
                        EditorialImage(
                            url = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC1A1512))))
                                .padding(EditorialSpace.sm)
                        ) {
                            Text(
                                text = item.title,
                                style = EditorialType.Caption,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfRail(
    books: List<PdfBook>,
    onBookClick: (PdfBook) -> Unit
) {
    if (books.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "বই ও সাময়িকী", subtitle = "ডাউনলোড করে পড়ুন")
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(books, key = { it.id }) { book ->
                Card(
                    onClick = { onBookClick(book) },
                    shape = RoundedCornerShape(EditorialShape.card),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.width(148.dp)
                ) {
                    Column {
                        EditorialImage(
                            url = book.coverUrl,
                            contentDescription = book.title,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .padding(EditorialSpace.sm)
                        )
                        Column(Modifier.padding(horizontal = EditorialSpace.md).padding(bottom = EditorialSpace.md)) {
                            Text(
                                text = book.title,
                                style = EditorialType.Subtitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (book.edition.isNotBlank()) {
                                Text(
                                    text = book.edition,
                                    style = EditorialType.Caption,
                                    color = LocalEditorialTokens.current.inkMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoRail(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit
) {
    if (videos.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "ভিডিও", subtitle = "নড়াচড়া ও কণ্ঠে সংস্কৃতি")
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(videos, key = { it.id }) { video ->
                Card(
                    onClick = { onVideoClick(video) },
                    shape = RoundedCornerShape(EditorialShape.card),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.width(252.dp)
                ) {
                    Column {
                        Box {
                            EditorialImage(
                                url = video.thumbnailUrl,
                                contentDescription = video.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )
                            Surface(
                                shape = RoundedCornerShape(EditorialShape.chip),
                                color = Color(0xCC1A1512),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(EditorialSpace.xs)
                            ) {
                                Text(
                                    text = video.platform,
                                    style = EditorialType.Caption,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = video.title,
                            style = EditorialType.Subtitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(EditorialSpace.md)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

@Composable
fun LoadingFeed(modifier: Modifier = Modifier, rows: Int = 5) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth().padding(EditorialSpace.gutter)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(EditorialShape.card))
                .background(brush)
        )
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = EditorialSpace.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
                Spacer(Modifier.width(EditorialSpace.md))
                Box(Modifier.size(width = 96.dp, height = 72.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.BrokenImage,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EditorialSpace.xl, vertical = EditorialSpace.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = LocalEditorialTokens.current.inkMuted,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(EditorialSpace.md))
        Text(
            text = message,
            style = EditorialType.Body,
            color = LocalEditorialTokens.current.inkSoft,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(EditorialSpace.md))
            TextButton(onClick = onAction) {
                Text(actionLabel, style = EditorialType.Subtitle, color = LocalEditorialTokens.current.accent)
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        message = message,
        modifier = modifier,
        actionLabel = "আবার চেষ্টা করুন",
        onAction = onRetry
    )
}

/** Convenience used by list screens: a plain-text teaser from stored HTML. */
fun teaserOf(html: String) = excerptOf(stripHtml(html), 140)
