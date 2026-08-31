package com.example.ui.editorial

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
import androidx.compose.ui.unit.dp
import com.example.ui.components.PortalAsyncImage
import com.example.ui.components.normalizePortalImageUrl

/**
 * Direct image component for editorial and reader views.
 * Loads immediately via Coil without viewport gating or delay.
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

    Box(modifier = modifier.clip(shape)) {
        PortalAsyncImage(
            url = cleaned,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
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
