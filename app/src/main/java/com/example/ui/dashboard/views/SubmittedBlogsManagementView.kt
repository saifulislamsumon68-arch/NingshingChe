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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.remote.SubmittedBlogRecord
import com.example.ui.dashboard.components.ConfirmDeleteDialog
import com.example.ui.dashboard.components.EmptyStateView
import com.example.ui.dashboard.components.StatusBadge

@Composable
fun SubmittedBlogsManagementView(
    submissions: List<SubmittedBlogRecord>,
    onApproveAndPublish: (SubmittedBlogRecord) -> Unit,
    onReject: (String) -> Unit,
    onDeleteSubmission: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var reviewingSubmission by remember { mutableStateOf<SubmittedBlogRecord?>(null) }
    var submissionToDelete by remember { mutableStateOf<SubmittedBlogRecord?>(null) }

    val filteredList = remember(submissions, searchQuery, selectedStatusFilter) {
        submissions.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.writerName.contains(searchQuery, ignoreCase = true) ||
                    item.content.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "PENDING" -> item.status.equals("Pending", ignoreCase = true)
                "PUBLISHED" -> item.status.equals("Published", ignoreCase = true)
                "REJECTED" -> item.status.equals("Rejected", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesStatus
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
                text = "পাঠক প্রেরিত লেখা ও পর্যালোচনা",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "মোট ${submissions.size} টি জমা (${submissions.count { it.status.equals("Pending", ignoreCase = true) }} টি অপেক্ষমাণ)",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("রচনার শিরোনাম বা লেখকের নাম দিয়ে খুঁজুন...") },
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
            modifier = Modifier.fillMaxWidth().testTag("dashboard_search_submissions")
        )

        // Status Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == "ALL",
                onClick = { selectedStatusFilter = "ALL" },
                label = { Text("সকল (${submissions.size})") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedStatusFilter == "PENDING",
                onClick = { selectedStatusFilter = "PENDING" },
                label = { Text("অপেক্ষমাণ (${submissions.count { it.status.equals("Pending", ignoreCase = true) }})") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedStatusFilter == "PUBLISHED",
                onClick = { selectedStatusFilter = "PUBLISHED" },
                label = { Text("প্রকাশিত (${submissions.count { it.status.equals("Published", ignoreCase = true) }})") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedStatusFilter == "REJECTED",
                onClick = { selectedStatusFilter = "REJECTED" },
                label = { Text("বাতিলকৃত (${submissions.count { it.status.equals("Rejected", ignoreCase = true) }})") },
                shape = RoundedCornerShape(10.dp)
            )
        }

        // List
        if (filteredList.isEmpty()) {
            EmptyStateView(
                message = if (searchQuery.isBlank()) "কোনো পাঠক লেখা জমা পাওয়া যায়নি।" else "\"$searchQuery\" সম্পর্কিত কোনো লেখা পাওয়া যায়নি।"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { sub ->
                    SubmittedBlogListItemCard(
                        submission = sub,
                        onReview = { reviewingSubmission = sub },
                        onApprove = { onApproveAndPublish(sub) },
                        onDelete = { submissionToDelete = sub }
                    )
                }
            }
        }
    }

    // Review Modal Dialog
    if (reviewingSubmission != null) {
        SubmissionReviewDialog(
            submission = reviewingSubmission!!,
            onDismiss = { reviewingSubmission = null },
            onApprove = {
                onApproveAndPublish(reviewingSubmission!!)
                reviewingSubmission = null
            },
            onReject = {
                onReject(reviewingSubmission!!.id)
                reviewingSubmission = null
            }
        )
    }

    // Delete Confirmation
    if (submissionToDelete != null) {
        ConfirmDeleteDialog(
            itemTitle = submissionToDelete?.title.orEmpty(),
            itemType = "লেখা",
            hasImgBbImage = submissionToDelete?.imgbbDeleteUrl?.isNotBlank() == true,
            onConfirm = {
                submissionToDelete?.id?.let { onDeleteSubmission(it) }
                submissionToDelete = null
            },
            onDismiss = { submissionToDelete = null }
        )
    }
}

@Composable
private fun SubmittedBlogListItemCard(
    submission: SubmittedBlogRecord,
    onReview: () -> Unit,
    onApprove: () -> Unit,
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
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PostAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Column {
                        Text(
                            text = submission.writerName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = submission.writerDesignation.ifBlank { submission.designation }.ifBlank { "পাঠক লেখক" },
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                StatusBadge(status = submission.status)
            }

            Text(
                text = submission.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = submission.content.replace(Regex("<[^>]*>"), ""),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onReview,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("dashboard_btn_review_submission_${submission.id}")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("পর্যালোচনা", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (submission.status.equals("Pending", ignoreCase = true)) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("dashboard_btn_approve_submission_${submission.id}")
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("অনুমোদন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("dashboard_btn_delete_submission_${submission.id}")
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

@Composable
private fun SubmissionReviewDialog(
    submission: SubmittedBlogRecord,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "পেছনে যান")
                            }
                            Text(
                                text = "পাঠক রচনা পর্যালোচনা",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        StatusBadge(status = submission.status)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Writer Details Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("লেখকের পরিচিতি ও তথ্য", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            Text("নাম: ${submission.writerName}", fontWeight = FontWeight.Bold)
                            if (submission.writerDesignation.isNotBlank()) Text("পদবি: ${submission.writerDesignation}")
                            if (submission.address.isNotBlank()) Text("ঠিকানা: ${submission.address}")
                            if (submission.phone.isNotBlank()) Text("ফোন: ${submission.phone}")
                            if (submission.writerEmail.isNotBlank()) Text("ইমেইল: ${submission.writerEmail}")
                        }
                    }

                    // Content Title
                    Text(
                        text = submission.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                    )

                    // Thumbnail if provided
                    if (submission.thumbnail.isNotBlank()) {
                        AsyncImage(
                            model = submission.thumbnail,
                            contentDescription = submission.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Article Content
                    Text(
                        text = submission.content.replace(Regex("<[^>]*>"), ""),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 26.sp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("dashboard_modal_btn_reject_sub")
                        ) {
                            Text("প্রত্যাখ্যান করুন")
                        }

                        Button(
                            onClick = onApprove,
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("dashboard_modal_btn_approve_sub")
                        ) {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("অনুমোদন ও ব্লগে প্রকাশ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
