package com.example.ui.dashboard.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.remote.GalleryRecord
import com.example.ui.dashboard.components.ConfirmDeleteDialog
import com.example.ui.dashboard.components.DashboardImageUploader
import com.example.ui.dashboard.components.EmptyStateView

@Composable
fun GalleriesManagementView(
    galleries: List<GalleryRecord>,
    onSaveGallery: (GalleryRecord) -> Unit,
    onDeleteGallery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("সব ছবি") }
    var editingGallery by remember { mutableStateOf<GalleryRecord?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var galleryToDelete by remember { mutableStateOf<GalleryRecord?>(null) }

    val categories = listOf("সব ছবি", "রাসোৎসব ও মেলা", "তাঁত ও হস্তশিল্প", "সাহিত্য ও দলিল", "সংস্কৃতি ও উৎসব")

    val filteredGalleries = remember(galleries, selectedCategory) {
        if (selectedCategory == "সব ছবি") galleries
        else galleries.filter { it.category.equals(selectedCategory, ignoreCase = true) }
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
                    text = "সাংস্কৃতিক আলোকচিত্র গ্যালারি",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "মোট ${galleries.size} টি আলোকচিত্র সংগৃহীত",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Button(
                onClick = { isCreatingNew = true },
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("dashboard_btn_add_gallery")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন ছবি", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Grid
        if (filteredGalleries.isEmpty()) {
            EmptyStateView(
                message = "কোনো ছবি পাওয়া যায়নি।",
                actionLabel = "ছবি যুক্ত করুন",
                onAction = { isCreatingNew = true }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredGalleries, key = { it.id }) { item ->
                    GalleryCardItem(
                        gallery = item,
                        onEdit = { editingGallery = item },
                        onDelete = { galleryToDelete = item }
                    )
                }
            }
        }
    }

    if (isCreatingNew || editingGallery != null) {
        val target = editingGallery ?: GalleryRecord(
            title = "",
            image = ""
        )
        GalleryFormDialog(
            initialGallery = target,
            isNew = isCreatingNew,
            onDismiss = {
                isCreatingNew = false
                editingGallery = null
            },
            onSave = { updated ->
                onSaveGallery(updated)
                isCreatingNew = false
                editingGallery = null
            }
        )
    }

    if (galleryToDelete != null) {
        ConfirmDeleteDialog(
            itemTitle = galleryToDelete?.title.orEmpty(),
            itemType = "ছবি",
            hasImgBbImage = galleryToDelete?.imgbbDeleteUrl?.isNotBlank() == true,
            onConfirm = {
                galleryToDelete?.id?.let { onDeleteGallery(it) }
                galleryToDelete = null
            },
            onDismiss = { galleryToDelete = null }
        )
    }
}

@Composable
private fun GalleryCardItem(
    gallery: GalleryRecord,
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
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                AsyncImage(
                    model = gallery.image,
                    contentDescription = gallery.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = gallery.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = gallery.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (gallery.description.isNotBlank()) {
                    Text(
                        text = gallery.description,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(34.dp).testTag("dashboard_edit_gallery_${gallery.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "সম্পাদনা",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp).testTag("dashboard_delete_gallery_${gallery.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "মুছুন",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryFormDialog(
    initialGallery: GalleryRecord,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (GalleryRecord) -> Unit
) {
    var title by remember { mutableStateOf(initialGallery.title) }
    var description by remember { mutableStateOf(initialGallery.description) }
    var image by remember { mutableStateOf(initialGallery.image) }
    var imgbbDeleteUrl by remember { mutableStateOf(initialGallery.imgbbDeleteUrl) }
    var category by remember { mutableStateOf(initialGallery.category) }
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
                    text = if (isNew) "নতুন ছবি যুক্ত করুন" else "ছবি সম্পাদনা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("ছবির শিরোনাম *") },
                    placeholder = { Text("যেমন: কমলগঞ্জের রাসোৎসব নৃত্য") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_gallery_title")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                DashboardImageUploader(
                    imageUrl = image,
                    onImageUrlChange = {
                        image = it
                        errorMessage = null
                    },
                    imgbbDeleteUrl = imgbbDeleteUrl,
                    onDeleteUrlChange = { imgbbDeleteUrl = it },
                    aspectRatio = 4f / 3f,
                    recommendedRatioText = "৪:৩ বা ১৬:৯ অনুপাত",
                    label = "আলোকচিত্র ছবি"
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("বিভাগ বা বিষয়") },
                    placeholder = { Text("যেমন: রাসোৎসব ও মেলা, তাঁতশিল্প") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_gallery_category")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("ছবির সংক্ষিপ্ত বিবরণ") },
                    placeholder = { Text("উৎসব বা ঐতিহ্যের প্রেক্ষাপট...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_gallery_description")
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
                                errorMessage = "ছবির শিরোনাম দেওয়া আবশ্যক"
                            } else if (image.isBlank()) {
                                errorMessage = "একটি ছবি যুক্ত করুন"
                            } else {
                                onSave(
                                    initialGallery.copy(
                                        title = title.trim(),
                                        description = description.trim(),
                                        image = image.trim(),
                                        imgbbDeleteUrl = imgbbDeleteUrl.trim(),
                                        category = category.ifBlank { "সংস্কৃতি ও উৎসব" }
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dashboard_btn_save_gallery")
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
