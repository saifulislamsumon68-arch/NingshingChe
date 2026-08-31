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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import coil.compose.AsyncImage
import com.example.data.remote.AuthorRecord
import com.example.ui.dashboard.components.ConfirmDeleteDialog
import com.example.ui.dashboard.components.DashboardImageUploader
import com.example.ui.dashboard.components.EmptyStateView
import com.example.ui.dashboard.components.VerifiedBadge

@Composable
fun AuthorsManagementView(
    authors: List<AuthorRecord>,
    onSaveAuthor: (AuthorRecord) -> Unit,
    onDeleteAuthor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingAuthor by remember { mutableStateOf<AuthorRecord?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var authorToDelete by remember { mutableStateOf<AuthorRecord?>(null) }

    val filteredAuthors = remember(authors, searchQuery) {
        if (searchQuery.isBlank()) authors
        else authors.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.designation.contains(searchQuery, ignoreCase = true) ||
                    it.location.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "লেখক ও গবেষক তালিকা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "মোট ${authors.size} জন নিবন্ধিত লেখক",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Button(
                onClick = { isCreatingNew = true },
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("dashboard_btn_add_author")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন লেখক", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("নাম বা পদবি দিয়ে খুঁজুন...") },
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
            modifier = Modifier.fillMaxWidth().testTag("dashboard_search_authors")
        )

        // Authors List
        if (filteredAuthors.isEmpty()) {
            EmptyStateView(
                message = if (searchQuery.isBlank()) "কোনো লেখক পাওয়া যায়নি।" else "\"$searchQuery\" দিয়ে কোনো লেখক খুঁজে পাওয়া যায়নি।",
                actionLabel = "নতুন লেখক যোগ করুন",
                onAction = { isCreatingNew = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAuthors, key = { it.id }) { author ->
                    AuthorListItemCard(
                        author = author,
                        onEdit = { editingAuthor = author },
                        onDelete = { authorToDelete = author }
                    )
                }
            }
        }
    }

    // Add / Edit Modal Dialog
    if (isCreatingNew || editingAuthor != null) {
        val target = editingAuthor ?: AuthorRecord(title = "")
        AuthorFormDialog(
            initialAuthor = target,
            isNew = isCreatingNew,
            onDismiss = {
                isCreatingNew = false
                editingAuthor = null
            },
            onSave = { updated ->
                onSaveAuthor(updated)
                isCreatingNew = false
                editingAuthor = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (authorToDelete != null) {
        ConfirmDeleteDialog(
            itemTitle = authorToDelete?.title.orEmpty(),
            itemType = "লেখক",
            hasImgBbImage = authorToDelete?.imgbbDeleteUrl?.isNotBlank() == true,
            onConfirm = {
                authorToDelete?.id?.let { onDeleteAuthor(it) }
                authorToDelete = null
            },
            onDismiss = { authorToDelete = null }
        )
    }
}

@Composable
private fun AuthorListItemCard(
    author: AuthorRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (author.image.isNotBlank()) {
                AsyncImage(
                    model = author.image,
                    contentDescription = author.title,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = author.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    VerifiedBadge(isVerified = author.isVerified)
                }

                if (author.designation.isNotBlank()) {
                    Text(
                        text = author.designation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (author.location.isNotBlank()) {
                    Text(
                        text = author.location,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("dashboard_edit_author_${author.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "সম্পাদনা",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("dashboard_delete_author_${author.id}")
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
private fun AuthorFormDialog(
    initialAuthor: AuthorRecord,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (AuthorRecord) -> Unit
) {
    var title by remember { mutableStateOf(initialAuthor.title) }
    var image by remember { mutableStateOf(initialAuthor.image) }
    var imgbbDeleteUrl by remember { mutableStateOf(initialAuthor.imgbbDeleteUrl) }
    var designation by remember { mutableStateOf(initialAuthor.designation) }
    var description by remember { mutableStateOf(initialAuthor.description) }
    var location by remember { mutableStateOf(initialAuthor.location) }
    var isVerified by remember { mutableStateOf(initialAuthor.isVerified) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isNew) "নতুন লেখক যুক্ত করুন" else "লেখক তথ্য সম্পাদনা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Name (Title)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("লেখকের নাম *") },
                    placeholder = { Text("যেমন: ব্রজেন্দ্রকুমার সিংহ") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_author_title")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Avatar / Image Uploader
                DashboardImageUploader(
                    imageUrl = image,
                    onImageUrlChange = { image = it },
                    imgbbDeleteUrl = imgbbDeleteUrl,
                    onDeleteUrlChange = { imgbbDeleteUrl = it },
                    aspectRatio = 1f,
                    recommendedRatioText = "১:১ অনুপাত",
                    label = "লেখকের ছবি"
                )

                // Designation
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("পদবি বা পরিচিতি") },
                    placeholder = { Text("যেমন: প্রাবন্ধিক ও গবেষক") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_author_designation")
                )

                // Description (Bio)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("লেখকের সংক্ষিপ্ত জীবনী") },
                    placeholder = { Text("সাহিত্য ও গবেষণার সংক্ষিপ্ত পরিচয়...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_author_bio")
                )

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("অবস্থান বা অঞ্চল") },
                    placeholder = { Text("যেমন: কমলগঞ্জ, মৌলভীবাজার") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_author_location")
                )

                // Is Verified Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "যাচাইকৃত লেখক",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "প্রোফাইলে ভেরিফাইড ব্যাজ প্রদর্শিত হবে",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = isVerified,
                        onCheckedChange = { isVerified = it },
                        modifier = Modifier.testTag("dashboard_switch_author_verified")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("বাতিল")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "লেখকের নাম দেওয়া আবশ্যক"
                            } else {
                                val result = initialAuthor.copy(
                                    title = title.trim(),
                                    image = image.trim(),
                                    imgbbDeleteUrl = imgbbDeleteUrl.trim(),
                                    designation = designation.trim(),
                                    description = description.trim(),
                                    location = location.trim(),
                                    isVerified = isVerified
                                )
                                onSave(result)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dashboard_btn_save_author")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
