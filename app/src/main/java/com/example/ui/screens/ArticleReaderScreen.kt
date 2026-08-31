package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.remote.NingshingCheWebsiteClient
import com.example.ui.components.ArticleListItemCard
import com.example.ui.components.ArticleReaderSkeleton
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalSaffron
import com.example.ui.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(
    articleId: String,
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onRelatedArticleClick: (String) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(articleId) { viewModel.loadArticle(articleId) }

    val article by viewModel.currentArticle.collectAsStateWithLifecycle()
    val relatedArticles by viewModel.relatedArticles.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val preferences by viewModel.readerPreferences.collectAsStateWithLifecycle()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentStatus by viewModel.commentStatus.collectAsStateWithLifecycle()
    val submitting by viewModel.isSubmittingComment.collectAsStateWithLifecycle()

    var showType by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    var isSkeletonLoading by remember(articleId) { mutableStateOf(true) }
    LaunchedEffect(articleId) {
        kotlinx.coroutines.delay(500L)
        isSkeletonLoading = false
    }

    if (article == null || isSkeletonLoading) {
        ArticleReaderSkeleton()
        return
    }

    val current = article!!
    val blocks = remember(current.content) { NingshingCheWebsiteClient.contentBlocks(current.content) }
    val scheme = MaterialTheme.colorScheme
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PortalSaffron,
        unfocusedBorderColor = scheme.outline,
        focusedContainerColor = scheme.surfaceVariant,
        unfocusedContainerColor = scheme.surfaceVariant,
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        cursorColor = PortalSaffron
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        Surface(color = scheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.testTag("reader_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.onSurface)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showType = true }) {
                    Icon(Icons.Default.FormatSize, contentDescription = "আকার", tint = scheme.onSurface)
                }
                IconButton(onClick = { viewModel.toggleTts() }) {
                    Icon(
                        if (isTtsPlaying) Icons.Default.Pause else Icons.Default.VolumeUp,
                        contentDescription = "শুনিক",
                        tint = if (isTtsPlaying) PortalSaffron else scheme.onSurface
                    )
                }
                IconButton(onClick = { viewModel.toggleBookmark() }) {
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "সংরক্ষণ",
                        tint = if (isBookmarked) PortalSaffron else scheme.onSurface
                    )
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${current.title}\n${current.sourceUrl}")
                    }
                    context.startActivity(Intent.createChooser(intent, "শেয়ার"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "শেয়ার", tint = scheme.onSurface)
                }
            }
        }

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .testTag("article_reader_content"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PortalSaffron
                ) {
                    Text(
                        text = current.category,
                        fontFamily = Kalpurush,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            item {
                Text(
                    text = current.title,
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    color = scheme.secondary,
                    fontSize = (preferences.fontSizeSp + 8).sp,
                    lineHeight = (preferences.fontSizeSp + 16).sp
                )
            }
            item {
                Text(
                    text = "ফঙিসিল: ${current.publishedDate}",
                    fontFamily = Kalpurush,
                    color = scheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAuthorClick(current.authorId) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = current.authorAvatarUrl.ifBlank { com.example.data.repository.NinghsingCheContentData.APP_LOGO_URL },
                            contentDescription = current.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(scheme.surfaceVariant)
                        )
                        if (com.example.data.remote.AuthorProfiles.isOfficial(current.authorAvatarUrl)) {
                            com.example.ui.components.VerifiedBadge(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                size = 16.dp
                            )
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(current.authorName, fontFamily = Kalpurush, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scheme.onSurface)
                            if (com.example.data.remote.AuthorProfiles.isOfficial(current.authorAvatarUrl)) {
                                com.example.ui.components.VerifiedBadge(size = 15.dp)
                            }
                        }
                        Text(
                            if (com.example.data.remote.AuthorProfiles.isOfficial(current.authorAvatarUrl)) "যাচাইকৃত লেখক • নিংশিং চে" else "লেখক • নিংশিং চে",
                            fontFamily = Kalpurush,
                            fontSize = 13.sp,
                            color = PortalSaffron
                        )
                    }
                }
            }
            if (current.featuredImageUrl.isNotBlank()) {
                item {
                    AsyncImage(
                        model = current.featuredImageUrl,
                        contentDescription = current.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            items(blocks.size) { index ->
                val (kind, value) = blocks[index]
                if (kind == "img") {
                    AsyncImage(
                        model = value,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Text(
                        text = value,
                        fontFamily = Kalpurush,
                        color = scheme.onBackground,
                        fontSize = preferences.fontSizeSp.sp,
                        lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Facebook",
                        color = PortalSaffron,
                        fontFamily = Kalpurush,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/sharer/sharer.php?u=${Uri.encode(current.sourceUrl)}"))
                            )
                        }
                    )
                    Text(
                        "WhatsApp",
                        color = PortalSaffron,
                        fontFamily = Kalpurush,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(current.title + " " + current.sourceUrl)}"))
                            )
                        }
                    )
                    Row(
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(current.sourceUrl)))
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = PortalSaffron, modifier = Modifier.size(14.dp))
                        Text("  মূল পাতা", color = PortalSaffron, fontFamily = Kalpurush, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (relatedArticles.isNotEmpty()) {
                item {
                    Text("নুয়া / মান্নাপা লেখা", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = scheme.secondary)
                }
                items(relatedArticles) { rel ->
                    ArticleListItemCard(rel, onClick = { onRelatedArticleClick(rel.id) })
                }
            }

            item {
                Text("হাব্বি মন্তব্যহানি", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = scheme.secondary)
            }
            if (comments.isEmpty()) {
                item {
                    Text("কোন মন্তব্য নেইসে।", fontFamily = Kalpurush, fontSize = 18.sp, color = scheme.onSurfaceVariant)
                }
            } else {
                items(comments) { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.surface,
                        border = BorderStroke(1.dp, scheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.name, fontFamily = Kalpurush, fontWeight = FontWeight.Bold, color = PortalSaffron)
                            Text(item.content, fontFamily = Kalpurush, fontSize = 15.sp, lineHeight = 24.sp, color = scheme.onSurface)
                        }
                    }
                }
            }

            item {
                Text("মন্তব্য করিক", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = scheme.secondary)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("নাঙহান *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                    OutlinedTextField(address, { address = it }, label = { Text("ঠিকানাহান", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                    OutlinedTextField(email, { email = it }, label = { Text("ইমেইল *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(phone, { phone = it }, label = { Text("ফোন নম্বর", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(
                        comment,
                        { comment = it },
                        label = { Text("মন্তব্য *", fontFamily = Kalpurush) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = fieldColors
                    )
                    if (!commentStatus.isNullOrBlank()) {
                        Text(commentStatus.orEmpty(), fontFamily = Kalpurush, color = PortalSaffron, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { viewModel.submitComment(name, address, email, phone, comment) },
                        enabled = !submitting,
                        colors = ButtonDefaults.buttonColors(containerColor = PortalSaffron),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (submitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                        else Text("মন্তব্য পাঠুইক", fontFamily = Kalpurush, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }

    if (showType) {
        ModalBottomSheet(onDismissRequest = { showType = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = scheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("অক্ষরের আকার", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, color = PortalSaffron)
                Slider(
                    value = preferences.fontSizeSp,
                    onValueChange = { viewModel.updateFontSize(it) },
                    valueRange = 14f..26f,
                    colors = SliderDefaults.colors(thumbColor = PortalSaffron, activeTrackColor = PortalSaffron)
                )
                Text("লাইনের ফাঁক", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, color = PortalSaffron)
                Slider(
                    value = preferences.lineSpacingMultiplier,
                    onValueChange = { viewModel.updateLineSpacing(it) },
                    valueRange = 1.3f..2.2f,
                    colors = SliderDefaults.colors(thumbColor = PortalSaffron, activeTrackColor = PortalSaffron)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
