package com.example.ui.dashboard.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.DashboardSummaryStats
import com.example.data.remote.RecentActivityItem
import com.example.data.remote.UserProfile
import com.example.ui.dashboard.components.DashboardSection
import com.example.ui.dashboard.components.DashboardStatCard
import com.example.ui.dashboard.components.StatusBadge

@Composable
fun DashboardHomeView(
    stats: DashboardSummaryStats,
    recentActivities: List<RecentActivityItem>,
    currentUser: UserProfile?,
    onNavigateSection: (DashboardSection) -> Unit,
    onQuickAddBlog: () -> Unit,
    onQuickAddAuthor: () -> Unit,
    onQuickAddPdf: () -> Unit,
    onQuickAddGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome & Quick Action Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "স্বাগতম, ${currentUser?.fullName ?: "অ্যাডমিন"}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "নিংশিং চে ডিজিটাল আর্কাইভ পোর্টাল",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "অনলাইন সক্রিয়",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                // Gorgeous Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onQuickAddBlog,
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dashboard_quick_add_blog")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("প্রবন্ধ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onQuickAddAuthor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dashboard_quick_add_author")
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("লেখক", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onQuickAddPdf,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dashboard_quick_add_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বই", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section Title
        Text(
            text = "সার্বিক পরিসংখ্যান",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        // Summary Statistics Grid
        // Row 1: Blogs & Views
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                title = "মোট প্রবন্ধ",
                count = "${stats.totalBlogs}",
                subtitle = "${stats.publishedBlogs} প্রকাশিত • ${stats.draftBlogs} খসড়া",
                icon = Icons.Default.MenuBook,
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = { onNavigateSection(DashboardSection.BLOGS) },
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "মোট পাঠক ভিউ",
                count = "${stats.totalViews}",
                subtitle = "আর্কাইভ পাঠক সংখ্যা",
                icon = Icons.Default.TrendingUp,
                iconColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Authors & Reader Submissions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                title = "লেখক তালিকা",
                count = "${stats.totalAuthors}",
                subtitle = "গবেষক ও সাহিত্যিক",
                icon = Icons.Default.People,
                iconColor = Color(0xFFE65100),
                onClick = { onNavigateSection(DashboardSection.AUTHORS) },
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "পাঠক রচনা",
                count = "${stats.pendingSubmittedBlogs}",
                subtitle = "নতুন জমাকৃত লেখা",
                icon = Icons.Default.PostAdd,
                iconColor = Color(0xFFC2185B),
                onClick = { onNavigateSection(DashboardSection.SUBMIT_BLOGS) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: PDF Books & Comments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                title = "বই ও সাময়িকী",
                count = "${stats.totalPdfBooks}",
                subtitle = "ডিজিটাল দলিল সংগ্রহ",
                icon = Icons.Default.PictureAsPdf,
                iconColor = Color(0xFFD32F2F),
                onClick = { onNavigateSection(DashboardSection.PDF_BOOKS) },
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "মন্তব্য ও মতামত",
                count = "${stats.totalComments}",
                subtitle = "${stats.publishedComments} প্রকাশিত",
                icon = Icons.Default.Comment,
                iconColor = Color(0xFF0288D1),
                onClick = { onNavigateSection(DashboardSection.COMMENTS) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 4: Categories, Galleries & Videos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                title = "বিষয় ও বিভাগ",
                count = "${stats.totalCategories}",
                subtitle = "সাহিত্য বিভাগ",
                icon = Icons.Default.Category,
                iconColor = Color(0xFF7B1FA2),
                onClick = { onNavigateSection(DashboardSection.CATEGORIES) },
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "ছবি গ্যালারি",
                count = "${stats.totalGalleries}",
                subtitle = "উৎসব ও তাঁতশিল্প",
                icon = Icons.Default.Collections,
                iconColor = Color(0xFF00796B),
                onClick = { onNavigateSection(DashboardSection.GALLERIES) },
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "ভিডিও সংগ্রহ",
                count = "${stats.totalVideos}",
                subtitle = "ডকুমেন্টারি",
                icon = Icons.Default.VideoLibrary,
                iconColor = Color(0xFFF57C00),
                onClick = { onNavigateSection(DashboardSection.VIDEOS) },
                modifier = Modifier.weight(1f)
            )
        }

        // Recent Activity Feed
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "সাম্প্রতিক কর্মকাণ্ড",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "সর্বশেষ আপডেট",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (recentActivities.isEmpty()) {
                    Text(
                        text = "কোনো সাম্প্রতিক কর্মকাণ্ড পাওয়া যায়নি।",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    recentActivities.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when (item.type) {
                                    "blog" -> MaterialTheme.colorScheme.primaryContainer
                                    "submission" -> MaterialTheme.colorScheme.tertiaryContainer
                                    "comment" -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (item.type) {
                                            "blog" -> Icons.Default.MenuBook
                                            "submission" -> Icons.Default.PostAdd
                                            "comment" -> Icons.Default.Comment
                                            "book" -> Icons.Default.PictureAsPdf
                                            else -> Icons.Default.Collections
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (item.status.isNotBlank()) {
                                StatusBadge(status = item.status)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
