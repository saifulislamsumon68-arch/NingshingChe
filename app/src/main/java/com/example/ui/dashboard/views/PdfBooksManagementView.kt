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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.remote.PdfBookRecord
import com.example.ui.dashboard.components.ConfirmDeleteDialog
import com.example.ui.dashboard.components.DashboardImageUploader
import com.example.ui.dashboard.components.EmptyStateView

@Composable
fun PdfBooksManagementView(
    pdfBooks: List<PdfBookRecord>,
    onSavePdfBook: (PdfBookRecord) -> Unit,
    onDeletePdfBook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingBook by remember { mutableStateOf<PdfBookRecord?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<PdfBookRecord?>(null) }

    val filteredBooks = remember(pdfBooks, searchQuery) {
        if (searchQuery.isBlank()) pdfBooks
        else pdfBooks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.authorOrEditor.contains(searchQuery, ignoreCase = true) ||
                    it.edition.contains(searchQuery, ignoreCase = true)
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
                    text = "পিডিএফ বই ও সাময়িকী",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "মোট ${pdfBooks.size} টি প্রকাশনা সংগৃহীত",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Button(
                onClick = { isCreatingNew = true },
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("dashboard_btn_add_pdf_book")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন বই", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("বইয়ের নাম বা সম্পাদক দিয়ে খুঁজুন...") },
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
            modifier = Modifier.fillMaxWidth().testTag("dashboard_search_pdf_books")
        )

        // List
        if (filteredBooks.isEmpty()) {
            EmptyStateView(
                message = if (searchQuery.isBlank()) "কোনো বই পাওয়া যায়নি।" else "\"$searchQuery\" সম্পর্কিত কোনো বই পাওয়া যায়নি।",
                actionLabel = "নতুন বই যুক্ত করুন",
                onAction = { isCreatingNew = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    PdfBookListItemCard(
                        book = book,
                        onEdit = { editingBook = book },
                        onDelete = { bookToDelete = book }
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isCreatingNew || editingBook != null) {
        val target = editingBook ?: PdfBookRecord(
            title = "",
            bookPublishedDate = "২০২৬"
        )
        PdfBookFormDialog(
            initialBook = target,
            isNew = isCreatingNew,
            onDismiss = {
                isCreatingNew = false
                editingBook = null
            },
            onSave = { updated ->
                onSavePdfBook(updated)
                isCreatingNew = false
                editingBook = null
            }
        )
    }

    // Delete Confirmation
    if (bookToDelete != null) {
        ConfirmDeleteDialog(
            itemTitle = bookToDelete?.title.orEmpty(),
            itemType = "বই",
            hasImgBbImage = bookToDelete?.imgbbDeleteUrl?.isNotBlank() == true,
            onConfirm = {
                bookToDelete?.id?.let { onDeletePdfBook(it) }
                bookToDelete = null
            },
            onDismiss = { bookToDelete = null }
        )
    }
}

@Composable
private fun PdfBookListItemCard(
    book: PdfBookRecord,
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
            if (book.image.isNotBlank()) {
                AsyncImage(
                    model = book.image,
                    contentDescription = book.title,
                    modifier = Modifier
                        .size(width = 75.dp, height = 55.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(width = 75.dp, height = 55.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "সম্পাদক: ${book.authorOrEditor.ifBlank { "নিংশিং চে পরিষদ" }} • ${book.bookPublishedDate}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.edition.isNotBlank()) {
                    Text(
                        text = "সংখ্যা: ${book.edition} • ${book.pageCount} পৃষ্ঠা",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp).testTag("dashboard_edit_pdf_${book.id}")
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
                    modifier = Modifier.size(36.dp).testTag("dashboard_delete_pdf_${book.id}")
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
private fun PdfBookFormDialog(
    initialBook: PdfBookRecord,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (PdfBookRecord) -> Unit
) {
    var title by remember { mutableStateOf(initialBook.title) }
    var image by remember { mutableStateOf(initialBook.image) }
    var imgbbDeleteUrl by remember { mutableStateOf(initialBook.imgbbDeleteUrl) }
    var bookPublishedDate by remember { mutableStateOf(initialBook.bookPublishedDate) }
    var link by remember { mutableStateOf(initialBook.link) }
    var authorOrEditor by remember { mutableStateOf(initialBook.authorOrEditor) }
    var edition by remember { mutableStateOf(initialBook.edition) }
    var category by remember { mutableStateOf(initialBook.category) }
    var pageCount by remember { mutableStateOf(initialBook.pageCount.toString()) }
    var fileSizeMb by remember { mutableStateOf(initialBook.fileSizeMb.toString()) }
    var description by remember { mutableStateOf(initialBook.description) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isNew) "নতুন বই / সাময়িকী যোগ" else "বই সম্পাদনা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("বই বা সাময়িকীর শিরোনাম *") },
                    placeholder = { Text("যেমন: নিংশিং চে - বার্ষিক সাহিত্য সংকলন") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_pdf_title")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Cover Uploader
                DashboardImageUploader(
                    imageUrl = image,
                    onImageUrlChange = { image = it },
                    imgbbDeleteUrl = imgbbDeleteUrl,
                    onDeleteUrlChange = { imgbbDeleteUrl = it },
                    aspectRatio = 2f / 1f,
                    recommendedRatioText = "২:১ অনুপাত (আড়াআড়ি কভার)",
                    label = "বইয়ের কভার ছবি"
                )

                // Date & Edition Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = bookPublishedDate,
                        onValueChange = { bookPublishedDate = it },
                        label = { Text("প্রকাশনার সাল") },
                        placeholder = { Text("যেমন: ২০২৬") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("dashboard_input_pdf_year")
                    )

                    OutlinedTextField(
                        value = edition,
                        onValueChange = { edition = it },
                        label = { Text("সংস্করণ / সংখ্যা") },
                        placeholder = { Text("যেমন: ১ম বর্ষ, ২য় সংখ্যা") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("dashboard_input_pdf_edition")
                    )
                }

                // Author / Editor
                OutlinedTextField(
                    value = authorOrEditor,
                    onValueChange = { authorOrEditor = it },
                    label = { Text("লেখক বা সম্পাদক") },
                    placeholder = { Text("যেমন: ব্রজেন্দ্রকুমার সিংহ (সম্পাদক)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_pdf_author")
                )

                // Download Link URL
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("পিডিএফ ফাইল সরাসরি পড়ার লিংক") },
                    placeholder = { Text("https://example.com/books/sample.pdf") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_pdf_download_link")
                )

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("শ্রেণি বা বিভাগ") },
                    placeholder = { Text("যেমন: বার্ষিক সাহিত্য সংকলন, ঐতিহাসিক দলিল") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Page Count & File Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = pageCount,
                        onValueChange = { pageCount = it },
                        label = { Text("পৃষ্ঠা সংখ্যা") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = fileSizeMb,
                        onValueChange = { fileSizeMb = it },
                        label = { Text("ফাইলের আকার (মেগাবাইট)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("বইয়ের সংক্ষিপ্ত ভূমিকা") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                                errorMessage = "বই বা সাময়িকীর নাম দেওয়া আবশ্যক"
                            } else {
                                onSave(
                                    initialBook.copy(
                                        title = title.trim(),
                                        image = image.trim(),
                                        imgbbDeleteUrl = imgbbDeleteUrl.trim(),
                                        bookPublishedDate = bookPublishedDate.trim(),
                                        link = link.trim(),
                                        authorOrEditor = authorOrEditor.trim(),
                                        edition = edition.trim(),
                                        category = category.ifBlank { "বার্ষিক সাহিত্য সংকলন" },
                                        pageCount = pageCount.toIntOrNull() ?: 0,
                                        fileSizeMb = fileSizeMb.toFloatOrNull() ?: 0f,
                                        description = description.trim()
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dashboard_btn_save_pdf_book")
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
