package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PdfViewerSkeletonLayout
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalSaffron
import com.example.ui.viewmodel.PdfViewerViewModel
import kotlinx.coroutines.launch

@Composable
fun PdfViewerScreen(
    pdfId: String,
    viewModel: PdfViewerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(pdfId) { viewModel.loadPdf(pdfId) }

    val pdfDocument by viewModel.pdfDocument.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()

    LaunchedEffect(downloadStatus) {
        downloadStatus?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearStatus()
        }
    }

    val pageCount = if (pages.isNotEmpty()) pages.size else 1
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF140A07), Color(0xFF2B1610), Color(0xFF0E0705))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("pdf_viewer_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFFFF3D6))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    pdfDocument?.title ?: "গ্রন্থাগার",
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFF3D6),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (pages.isNotEmpty()) "পৃষ্ঠা ${pagerState.currentPage + 1} / $pageCount" else (pdfDocument?.edition ?: ""),
                    fontFamily = Kalpurush,
                    color = PortalSaffron,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { viewModel.sharePdf() }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = PortalSaffron)
            }
            IconButton(onClick = { viewModel.downloadPdf() }) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = PortalSaffron)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                isLoading -> PdfViewerSkeletonLayout()
                pages.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = PortalSaffron, modifier = Modifier.size(56.dp))
                    Text("বইটি খোলা যায়নি", fontFamily = Kalpurush, color = Color(0xFFFFF3D6))
                }
                else -> HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var offsetY by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(18.dp, RoundedCornerShape(2.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFFFF8EC))
                                .border(1.dp, Color(0xFFD7B48A), RoundedCornerShape(2.dp))
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 3.5f)
                                        if (scale > 1f) {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                }
                        ) {
                            Box(
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxWidth(0.03f)
                                    .height(10000.dp)
                                    .background(Brush.horizontalGradient(listOf(Color(0x332A140E), Color.Transparent)))
                            )
                            if (pageIndex < pages.size) {
                                Image(
                                    bitmap = pages[pageIndex].asImageBitmap(),
                                    contentDescription = "পৃষ্ঠা ${pageIndex + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B2418))
                ) {
                    Icon(Icons.Default.NavigateBefore, null, tint = PortalSaffron)
                }
                Text(
                    "পৃষ্ঠা ${pagerState.currentPage + 1} / $pageCount",
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFF3D6)
                )
                IconButton(
                    onClick = {
                        if (pagerState.currentPage < pageCount - 1) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    enabled = pagerState.currentPage < pageCount - 1,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B2418))
                ) {
                    Icon(Icons.Default.NavigateNext, null, tint = PortalSaffron)
                }
            }
        }
    }
}
