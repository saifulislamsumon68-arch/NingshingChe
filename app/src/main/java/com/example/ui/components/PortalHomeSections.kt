package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Article
import com.example.data.model.Author
import com.example.data.model.Category
import com.example.data.model.PdfDocument
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalSaffron

@Composable
fun PortalSectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = Kalpurush,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                fontFamily = Kalpurush,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = PortalSaffron,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
fun FeaturedPortalCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(292.dp)
            .clickable(onClick = onClick)
            .testTag("featured_portal_${article.id}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PortalAsyncImage(
                url = article.featuredImageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = PortalSaffron, shape = RoundedCornerShape(50)) {
                            Text(
                                "ফিচার্ড",
                                fontFamily = Kalpurush,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            article.publishedDate,
                            fontFamily = Kalpurush,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = article.title,
                        fontFamily = Kalpurush,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = article.authorName,
                    fontFamily = Kalpurush,
                    fontSize = 12.sp,
                    color = PortalSaffron,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SelectedEssayCard(
    article: Article,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier
            .width(248.dp)
            .height(292.dp)
            .clickable(onClick = onClick)
            .testTag("selected_essay_${article.id}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PortalAsyncImage(
                url = article.featuredImageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(color = PortalSaffron, shape = RoundedCornerShape(50)) {
                        Text(
                            article.category,
                            fontFamily = Kalpurush,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        article.publishedDate,
                        fontFamily = Kalpurush,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    article.title,
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "লেখক: ${article.authorName}",
                    fontFamily = Kalpurush,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CategoryImageTile(
    category: Category,
    imageUrl: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .width(148.dp)
            .clickable(onClick = onClick)
            .testTag("category_tile_${category.slug}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            ) {
                PortalAsyncImage(
                    url = imageUrl,
                    contentDescription = category.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f))
                )
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    category.name,
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${category.articleCount}গো লেখা",
                    fontFamily = Kalpurush,
                    fontSize = 11.sp,
                    color = PortalSaffron
                )
            }
        }
    }
}

@Composable
fun AuthorRailCard(
    author: Author,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .width(168.dp)
            .clickable(onClick = onClick)
            .testTag("author_rail_${author.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                PortalAsyncImage(
                    url = author.avatarUrl,
                    contentDescription = author.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                if (author.isVerified) {
                    VerifiedBadge(modifier = Modifier.align(Alignment.BottomEnd), size = 20.dp)
                }
            }
            Text(
                author.name,
                fontFamily = Kalpurush,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                author.designation,
                fontFamily = Kalpurush,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "প্রোফাইলগো চেইক",
                fontFamily = Kalpurush,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PortalSaffron
            )
        }
    }
}

@Composable
fun PdfBookRailCard(
    pdf: PdfDocument,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .width(168.dp)
            .height(268.dp)
            .clickable(onClick = onClick)
            .testTag("pdf_rail_${pdf.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PortalAsyncImage(
                url = pdf.coverImageUrl,
                contentDescription = pdf.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(108.dp)
                    .height(144.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
            Text(
                pdf.title,
                fontFamily = Kalpurush,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (pdf.year > 0) "ফঙসিলঃ ${pdf.year}" else pdf.edition,
                fontFamily = Kalpurush,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                "পাকরিক",
                fontFamily = Kalpurush,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = PortalSaffron
            )
        }
    }
}

@Composable
fun HorizontalCardsRow(
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun SubmitWritingBanner(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
            .testTag("home_submit_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = PortalSaffron, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "লেখা জমাদান",
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Text(
                    "নিংশিং চে-ত আর্টিকেল পাঠুইক — নাম, পরিচিতি বারো লেখা",
                    fontFamily = Kalpurush,
                    fontSize = 12.sp,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}
