package com.example.ui.dashboard.views

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.CommentRecord
import com.example.ui.dashboard.components.ConfirmDeleteDialog
import com.example.ui.dashboard.components.EmptyStateView
import com.example.ui.dashboard.components.StatusBadge

@Composable
fun CommentsManagementView(
    comments: List<CommentRecord>,
    onToggleStatus: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var commentToDelete by remember { mutableStateOf<CommentRecord?>(null) }

    val filteredComments = remember(comments, searchQuery, selectedFilter) {
        comments.filter { comment ->
            val matchesQuery = searchQuery.isBlank() ||
                    comment.name.contains(searchQuery, ignoreCase = true) ||
                    comment.email.contains(searchQuery, ignoreCase = true) ||
                    comment.content.contains(searchQuery, ignoreCase = true) ||
                    comment.blogTitle.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "PUBLISH" -> comment.isPublished
                "UNPUBLISH" -> !comment.isPublished
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Column {
            Text(
                text = "পাঠক মন্তব্য ও প্রতিক্রিয়া",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "মোট ${comments.size} টি মন্তব্য (${comments.count { it.isPublished }} টি প্রকাশিত)",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("মন্তব্যকারী বা বিষয় দিয়ে খুঁজুন...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth().testTag("dashboard_search_comments")
        )

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("সকল (${comments.size})") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedFilter == "PUBLISH",
                onClick = { selectedFilter = "PUBLISH" },
                label = { Text("প্রকাশিত (${comments.count { it.isPublished }})") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedFilter == "UNPUBLISH",
                onClick = { selectedFilter = "UNPUBLISH" },
                label = { Text("অপেক্ষমাণ (${comments.count { !it.isPublished }})") },
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Comments List
        if (filteredComments.isEmpty()) {
            EmptyStateView(
                message = if (searchQuery.isBlank()) "কোনো মন্তব্য নেই।" else "\"$searchQuery\" সম্পর্কিত কোনো মন্তব্য পাওয়া যায়নি।"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredComments, key = { it.id }) { comment ->
                    CommentItemCard(
                        comment = comment,
                        onToggleStatus = { onToggleStatus(comment.id) },
                        onDelete = { commentToDelete = comment }
                    )
                }
            }
        }
    }

    if (commentToDelete != null) {
        ConfirmDeleteDialog(
            itemTitle = "${commentToDelete?.name}-এর মন্তব্য",
            itemType = "মন্তব্য",
            onConfirm = {
                commentToDelete?.id?.let { onDeleteComment(it) }
                commentToDelete = null
            },
            onDismiss = { commentToDelete = null }
        )
    }
}

@Composable
private fun CommentItemCard(
    comment: CommentRecord,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Column {
                        Text(
                            text = comment.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (comment.email.isNotBlank() || comment.phone.isNotBlank()) {
                            Text(
                                text = listOf(comment.email, comment.phone, comment.address).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            )
                        }
                    }
                }

                StatusBadge(status = comment.status)
            }

            if (comment.blogTitle.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "প্রবন্ধ: ${comment.blogTitle}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                lineHeight = 20.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onToggleStatus,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("dashboard_btn_toggle_comment_${comment.id}")
                ) {
                    Icon(
                        imageVector = if (comment.isPublished) Icons.Default.Unpublished else Icons.Default.PublishedWithChanges,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (comment.isPublished) "অপ্রকাশিত করুন" else "অনুমোদন ও প্রকাশ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("dashboard_btn_delete_comment_${comment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "মুছুন",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
