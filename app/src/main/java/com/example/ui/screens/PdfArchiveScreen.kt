package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PdfDocument
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalMaroon
import com.example.ui.theme.PortalSaffron
import com.example.ui.viewmodel.PdfArchiveViewModel
import com.example.util.PdfHelper
import kotlinx.coroutines.launch

@Composable
fun PdfArchiveScreen(
    viewModel: PdfArchiveViewModel,
    onNavigateBack: () -> Unit,
    onOpenPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfs by viewModel.filteredPdfs.collectAsState()
    val shelves = remember(pdfs) { pdfs.chunked(3).ifEmpty { listOf(emptyList()) } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2A140E), Color(0xFF4A2216), Color(0xFF1A0C08))
                )
            ),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AutoStories, null, tint = PortalSaffron, modifier = Modifier.size(28.dp))
                    Text("ডিজিটাল গ্রন্থাগার", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color(0xFFFFF3D6))
                }
                Text(
                    "নিংশিং চে মুদ্রিত সংখ্যা ও স্মারকপত্র — তাক থেকে একটি বই তুলুন",
                    fontFamily = Kalpurush,
                    color = Color(0xFFE7C9A0),
                    fontSize = 14.sp
                )
            }
        }

        shelves.forEachIndexed { index, row ->
            item {
                LibraryShelf(
                    books = row,
                    shelfLabel = if (index == 0) "মূল তাক" else "তাক ${index + 1}",
                    onOpen = onOpenPdf,
                    onDownload = { doc ->
                        scope.launch {
                            Toast.makeText(context, "ডাউনলোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                            val msg = PdfHelper.savePdfToDownloads(context, doc).getOrElse { "ডাউনলোড হয়নি" }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryShelf(
    books: List<PdfDocument>,
    shelfLabel: String,
    onOpen: (String) -> Unit,
    onDownload: (PdfDocument) -> Unit
) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(
            shelfLabel,
            fontFamily = Kalpurush,
            color = Color(0xFFE8C48A),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(books, key = { it.id }) { book ->
                LibraryBook(book, onOpen = { onOpen(book.id) }, onDownload = { onDownload(book) })
            }
        }
        Box(
            Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF8B5A2B), Color(0xFF5C3310), Color(0xFF3B1E0A))))
        )
    }
}

@Composable
private fun LibraryBook(
    book: PdfDocument,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .height(176.dp)
                .shadow(10.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(PortalMaroon)
        ) {
            com.example.ui.components.PortalAsyncImage(
                url = book.coverImageUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC2A140E))
                        )
                    )
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(8.dp)
                    .height(176.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0x662A140E), Color.Transparent)))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    if (book.year > 0) "${book.year}" else "PDF",
                    color = PortalSaffron,
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    book.title,
                    color = Color(0xFFFFF6E4),
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PortalSaffron)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MenuBook, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Text(" পাকরিক", color = Color.White, fontFamily = Kalpurush, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Icon(
                Icons.Default.Download,
                contentDescription = "ডাউনলোড",
                tint = Color(0xFFE8C48A),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDownload)
            )
        }
    }
}
