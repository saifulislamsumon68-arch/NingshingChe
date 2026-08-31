package com.example.ui.editorial

import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.R
import com.example.ui.components.normalizePortalImageUrl

/**
 * Loads the image immediately when the composable enters the composition.
 *
 * This intentionally does not use viewport/geometry-based lazy loading. The previous
 * gate delayed requests in scrolling lists and made images appear too slowly. Coil's
 * memory and disk caches still prevent unnecessary downloads.
 */
@Composable
fun LazyImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(EditorialShape.thumb)
) {
    val cleaned = remember(url) { normalizePortalImageUrl(url) }
    if (cleaned.isBlank()) {
        ImagePlaceholder(modifier = modifier, shape = shape)
        return
    }

    val context = LocalContext.current
    val request = remember(cleaned) {
        ImageRequest.Builder(context)
            .data(cleaned)
            .crossfade(true)
            .addHeader("Referer", "https://ningshingche.com/")
            .addHeader("User-Agent", PORTAL_IMAGE_UA)
            .addHeader("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            .error(R.drawable.ic_ningshingche_logo)
            .fallback(R.drawable.ic_ningshingche_logo)
            .build()
    }

    val painter = rememberAsyncImagePainter(model = request)
    Box(modifier = modifier.clip(shape)) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> ComposeImage(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
            is AsyncImagePainter.State.Error -> ImagePlaceholder(
                modifier = Modifier.fillMaxSize(), shape = shape, broken = true
            )
            else -> ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(rememberShimmerBrush()))
}

@Composable
fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(EditorialShape.thumb),
    broken: Boolean = false
) {
    val tokens = LocalEditorialTokens.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.surfaceSunken)
            .border(1.dp, tokens.rule, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (broken) Icons.Default.BrokenImage else Icons.Default.Image,
            contentDescription = null,
            tint = tokens.inkMuted,
            modifier = Modifier.fillMaxSize(0.35f)
        )
    }
}

internal const val PORTAL_IMAGE_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
