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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.remote.AuthorRecord
import com.example.data.remote.BlogRecord
import com.example.data.remote.CategoryRecord
import com.example.ui.dashboard.components.BlogPreviewDialog
import com.example.ui.dashboard.components.ConfirmDeleteDialog
import com.example.ui.dashboard.components.DashboardImageUploader
import com.example.ui.dashboard.components.DashboardRichTextEditor
import com.example.ui.dashboard.components.DashboardTagInput
import com.example.ui.dashboard.components.EmptyStateView
import com.example.ui.dashboard.components.StatusBadge

@Composable
fun BlogsManagementView(
    blogs: List<BlogRecord>,
    categories: List<CategoryRecord>,
    authors: List<AuthorRecord>,
    onSaveBlog: (BlogRecord) -> Unit,
    onDeleteBlog: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var editingBlog by remember { mutableStateOf<BlogRecord?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var previewingBlog by remember { mutableStateOf<BlogRecord?>(null) }
    var blogToDelete by remember { mutableStateOf<BlogRecord?>(null) }

    val filteredBlogs = remember(blogs, searchQuery, selectedStatusFilter) {
        blogs.filter { blog ->
            val matchesQuery = searchQuery.isBlank() ||
                    blog.title.contains(searchQuery, ignoreCase = true) ||
                    blog.authorName.contains(searchQuery, ignoreCase = true) ||
                    blog.categoryTitle.contains(searchQuery, ignoreCase = true) ||
                    blog.tags.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesStatus = when (selectedStatusFilter) {
                "PUBLISH" -> blog.isPublished
                "DRAFT" -> !blog.isPublished
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "প্রবন্ধ ও রচনা তালিকা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "মোট ${blogs.size} টি (${blogs.count { it.isPublished }} টি প্রকাশিত)",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Button(
                onClick = { isCreatingNew = true },
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("dashboard_btn_add_blog")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন রচনা", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("শিরোনাম, লেখক বা বিষয় দিয়ে খুঁজুন...") },
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
            modifier = Modifier.fillMaxWidth().testTag("dashboard_search_blogs")
        )

        // Status Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == "ALL",
                onClick = { selectedStatusFilter = "ALL" },
                label = { Text("সকল (${blogs.size})") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedStatusFilter == "PUBLISH",
                onClick = { selectedStatusFilter = "PUBLISH" },
                label = { Text("প্রকাশিত (${blogs.count { it.isPublished }})") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedStatusFilter == "DRAFT",
                onClick = { selectedStatusFilter = "DRAFT" },
                label = { Text("খসড়া (${blogs.count { !it.isPublished }})") },
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Blog Items List
        if (filteredBlogs.isEmpty()) {
            EmptyStateView(
                message = if (searchQuery.isBlank()) "কোনো প্রবন্ধ পাওয়া যায়নি।" else "\"$searchQuery\" দিয়ে কোনো প্রবন্ধ পাওয়া যায়নি।",
                actionLabel = "নতুন প্রবন্ধ তৈরি করুন",
                onAction = { isCreatingNew = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBlogs, key = { it.id }) { blog ->
                    BlogListItemCard(
                        blog = blog,
                        onPreview = { previewingBlog = blog },
                        onEdit = { editingBlog = blog },
                        onDelete = { blogToDelete = blog }
                    )
                }
            }
        }
    }

    // Add / Edit Blog Dialog
    if (isCreatingNew || editingBlog != null) {
        val target = editingBlog ?: BlogRecord(
            title = "",
            content = "",
            slug = "article-${System.currentTimeMillis() % 100000}",
            categoryId = categories.firstOrNull()?.id.orEmpty(),
            categoryTitle = categories.firstOrNull()?.title.orEmpty(),
            categorySlug = categories.firstOrNull()?.slug.orEmpty(),
            authorId = authors.firstOrNull()?.id.orEmpty(),
            authorName = authors.firstOrNull()?.title.orEmpty(),
            authorImage = authors.firstOrNull()?.image.orEmpty(),
            status = "Publish"
        )

        BlogFormDialog(
            initialBlog = target,
            categories = categories,
            authors = authors,
            isNew = isCreatingNew,
            onDismiss = {
                isCreatingNew = false
                editingBlog = null
            },
            onSave = { updated ->
                onSaveBlog(updated)
                isCreatingNew = false
                editingBlog = null
            }
        )
    }

    // Public Reader Preview Modal
    if (previewingBlog != null) {
        BlogPreviewDialog(
            blog = previewingBlog!!,
            onDismiss = { previewingBlog = null }
        )
    }

    // Delete Confirmation Dialog
    if (blogToDelete != null) {
        ConfirmDeleteDialog(
            itemTitle = blogToDelete?.title.orEmpty(),
            itemType = "প্রবন্ধ",
            onConfirm = {
                blogToDelete?.id?.let { onDeleteBlog(it) }
                blogToDelete = null
            },
            onDismiss = { blogToDelete = null }
        )
    }
}

@Composable
private fun BlogListItemCard(
    blog: BlogRecord,
    onPreview: () -> Unit,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (blog.image.isNotBlank()) {
                AsyncImage(
                    model = blog.image,
                    contentDescription = blog.title,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = blog.title.take(1).ifBlank { "প্র" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(isPublished = blog.isPublished)

                    if (blog.isSlider) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "স্লাইডার",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = blog.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${blog.authorName.ifBlank { "সম্পাদক" }} • ${blog.categoryTitle}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onPreview,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("dashboard_preview_blog_${blog.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "প্রিভিউ",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("dashboard_edit_blog_${blog.id}")
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
                        .size(36.dp)
                        .testTag("dashboard_delete_blog_${blog.id}")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlogFormDialog(
    initialBlog: BlogRecord,
    categories: List<CategoryRecord>,
    authors: List<AuthorRecord>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (BlogRecord) -> Unit
) {
    var title by remember { mutableStateOf(initialBlog.title) }
    var subTitle by remember { mutableStateOf(initialBlog.subTitle) }
    var slug by remember { mutableStateOf(initialBlog.slug) }
    var content by remember { mutableStateOf(initialBlog.content) }
    var image by remember { mutableStateOf(initialBlog.image) }

    var selectedCategoryId by remember { mutableStateOf(initialBlog.categoryId.ifBlank { categories.firstOrNull()?.id.orEmpty() }) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedAuthorId by remember { mutableStateOf(initialBlog.authorId.ifBlank { authors.firstOrNull()?.id.orEmpty() }) }
    var authorDropdownExpanded by remember { mutableStateOf(false) }

    var status by remember { mutableStateOf(initialBlog.status) }
    var isSlider by remember { mutableStateOf(initialBlog.isSlider) }
    var isFeature by remember { mutableStateOf(initialBlog.isFeature) }
    var isSpecialArticle by remember { mutableStateOf(initialBlog.isSpecialArticle) }
    var tags by remember { mutableStateOf(initialBlog.tags) }
    var videoLink by remember { mutableStateOf(initialBlog.videoLink) }
    var pdfBookLink by remember { mutableStateOf(initialBlog.pdfBookLink) }
    var publishedDate by remember { mutableStateOf(initialBlog.publishedDate.ifBlank { "২০২৬" }) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
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
                        Text(
                            text = if (isNew) "নতুন রচনা তৈরি" else "রচনা সম্পাদনা",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("বাতিল")
                            }

                            Button(
                                onClick = {
                                    if (title.isBlank()) {
                                        errorMessage = "শিরোনাম দেওয়া আবশ্যক"
                                    } else if (content.isBlank()) {
                                        errorMessage = "মূল রচনা বা বিষয়বস্তু লিখুন"
                                    } else {
                                        val cat = categories.find { it.id == selectedCategoryId }
                                        val aut = authors.find { it.id == selectedAuthorId }

                                        val generatedSlug = if (slug.isNotBlank()) slug
                                        else title.lowercase().replace(Regex("[^a-zA-Z0-9\\u0980-\\u09FF]+"), "-").trim('-')

                                        val updated = initialBlog.copy(
                                            title = title.trim(),
                                            subTitle = subTitle.trim(),
                                            slug = generatedSlug,
                                            content = content,
                                            image = image.trim(),
                                            categoryId = cat?.id.orEmpty(),
                                            categoryTitle = cat?.title.orEmpty(),
                                            categorySlug = cat?.slug.orEmpty(),
                                            authorId = aut?.id.orEmpty(),
                                            authorName = aut?.title.orEmpty(),
                                            authorImage = aut?.image.orEmpty(),
                                            status = status,
                                            isSlider = isSlider,
                                            isFeature = isFeature,
                                            isSpecialArticle = isSpecialArticle,
                                            tags = tags,
                                            videoLink = videoLink.trim(),
                                            pdfBookLink = pdfBookLink.trim(),
                                            publishedDate = publishedDate.trim()
                                        )
                                        onSave(updated)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("dashboard_btn_save_blog")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("সংরক্ষণ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            errorMessage = null
                            if (slug.isBlank() || slug.startsWith("article-")) {
                                slug = it.lowercase().replace(Regex("[^a-zA-Z0-9\\u0980-\\u09FF]+"), "-").trim('-')
                            }
                        },
                        label = { Text("প্রবন্ধের শিরোনাম *") },
                        placeholder = { Text("যেমন: বিষ্ণুপ্রিয়া মণিপুরি ভাষার প্রাচীন সাহিত্য ও ঐতিহ্য") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dashboard_input_blog_title")
                    )

                    // Subtitle
                    OutlinedTextField(
                        value = subTitle,
                        onValueChange = { subTitle = it },
                        label = { Text("উপ-শিরোনাম বা সংক্ষিপ্ত সারাংশ") },
                        placeholder = { Text("সংক্ষিপ্ত ২-৩ লাইনের ভূমিকা...") },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dashboard_input_blog_subtitle")
                    )

                    // Slug & Published Year Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = slug,
                            onValueChange = { slug = it },
                            label = { Text("ইউআরএল স্লাগ") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("dashboard_input_blog_slug")
                        )

                        OutlinedTextField(
                            value = publishedDate,
                            onValueChange = { publishedDate = it },
                            label = { Text("প্রকাশনার সাল / সময়") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("dashboard_input_blog_date")
                        )
                    }

                    // Category & Author Selectors Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Category Dropdown
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            val selectedCat = categories.find { it.id == selectedCategoryId }
                            OutlinedTextField(
                                value = selectedCat?.title ?: "বিভাগ নির্বাচন করুন",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("বিভাগ") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.title) },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Author Dropdown
                        ExposedDropdownMenuBox(
                            expanded = authorDropdownExpanded,
                            onExpandedChange = { authorDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            val selectedAuthor = authors.find { it.id == selectedAuthorId }
                            OutlinedTextField(
                                value = selectedAuthor?.title ?: "লেখক নির্বাচন করুন",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("লেখক") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authorDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = authorDropdownExpanded,
                                onDismissRequest = { authorDropdownExpanded = false }
                            ) {
                                authors.forEach { aut ->
                                    DropdownMenuItem(
                                        text = { Text(aut.title) },
                                        onClick = {
                                            selectedAuthorId = aut.id
                                            authorDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Featured Image Uploader
                    DashboardImageUploader(
                        imageUrl = image,
                        onImageUrlChange = { image = it },
                        aspectRatio = 16f / 9f,
                        recommendedRatioText = "১৬:৯ অনুপাত",
                        label = "প্রবন্ধের প্রধান ছবি"
                    )

                    // Rich Text Content Editor
                    DashboardRichTextEditor(
                        content = content,
                        onContentChange = {
                            content = it
                            errorMessage = null
                        }
                    )

                    // Tags Input Component
                    DashboardTagInput(
                        tags = tags,
                        onTagsChange = { tags = it }
                    )

                    // Display settings
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "প্রকাশনা নিয়ন্ত্রণ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            // Status Radio (Publish vs Draft)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = status == "Publish",
                                    onClick = { status = "Publish" },
                                    label = { Text("সরাসরি প্রকাশ") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = status == "Draft",
                                    onClick = { status = "Draft" },
                                    label = { Text("খসড়া সংরক্ষণ") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Hero Carousel Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("শীর্ষ স্লাইডার", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("হোমপেজের শীর্ষ স্লাইডারে প্রদর্শিত হবে", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isSlider,
                                    onCheckedChange = { isSlider = it },
                                    modifier = Modifier.testTag("dashboard_switch_is_slider")
                                )
                            }

                            // Featured Articles Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("বিশেষ ফিচার্ড রচনা", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("হোমপেজে বিশেষভাবে হাইলাইট হবে", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isFeature,
                                    onCheckedChange = { isFeature = it },
                                    modifier = Modifier.testTag("dashboard_switch_is_featured")
                                )
                            }

                            // Special Article Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("সম্পাদকীয় নির্বাচন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("বিশেষ সাংস্কৃতিক ও ঐতিহাসিক দলিল হিসেবে চিহ্নিত", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isSpecialArticle,
                                    onCheckedChange = { isSpecialArticle = it },
                                    modifier = Modifier.testTag("dashboard_switch_is_special")
                                )
                            }
                        }
                    }

                    // Extra Metadata (Video link, PDF book link)
                    OutlinedTextField(
                        value = videoLink,
                        onValueChange = { videoLink = it },
                        label = { Text("ভিডিও লিংক (ঐচ্ছিক)") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("dashboard_input_blog_video_link")
                    )

                    OutlinedTextField(
                        value = pdfBookLink,
                        onValueChange = { pdfBookLink = it },
                        label = { Text("সংযুক্ত পিডিএফ বই লিংক (ঐচ্ছিক)") },
                        placeholder = { Text("https://example.com/sample.pdf") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("dashboard_input_blog_pdf_link")
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
