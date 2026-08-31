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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.R
import com.example.ui.components.normalizePortalImageUrl

/**
 * An image that is **not fetched until it is close to the viewport**, and that shows a
 * shimmering skeleton over the reserved space until the bitmap is fully decoded.
 *
 * Two separate ideas are combined here, because both were asked for:
 *
 * 1. *Lazy loading* — `LazyColumn`/`LazyRow` only compose what is near the screen, but a
 *    tall card inside one item still pulls every image it contains the moment that item
 *    is composed. [preloadMargin] gates the Coil request behind an actual geometry check,
 *    so off-screen images never touch the network. Once armed, an image stays armed for
 *    the lifetime of the composable, so scrolling back up does not re-trigger a fetch.
 *
 * 2. *Skeleton until loaded* — the painter state drives the swap. The skeleton covers
 *    both "not armed yet" and `Loading`, and the real bitmap is only drawn on
 *    [AsyncImagePainter.State.Success], i.e. once it is 100% loaded. `crossfade` then
 *    fades it in over the skeleton.
 *
 * The box keeps its size in every state, so lists never jump when an image arrives.
 */
@Composable
fun LazyImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(EditorialShape.thumb),
    preloadMargin: Dp = 150.dp
) {
    val cleaned = remember(url) { normalizePortalImageUrl(url) }
    if (cleaned.isBlank()) {
        ImagePlaceholder(modifier = modifier, shape = shape)
        return
    }

    var armed by remember(cleaned) { mutableStateOf(false) }
    val density = LocalDensity.current
    val preloadPx = remember(preloadMargin, density) { with(density) { preloadMargin.toPx() } }

    // Geometry gate. Only installed while unarmed, so we stop measuring once resolved.
    val gate: Modifier = if (armed) {
        Modifier
    } else {
        Modifier.onGloballyPositioned { coords ->
            val root = coords.findRootCoordinates()
            val rect = coords.boundsInRoot()
            val rootHeight = root.size.height.toFloat()
            if (rect.bottom >= -preloadPx && rect.top <= rootHeight + preloadPx) {
                armed = true
            }
        }
    }

    Box(modifier = modifier.clip(shape).then(gate)) {
        if (!armed) {
            ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
        } else {
            val context = LocalContext.current
            val request = remember(cleaned, context) {
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
            when (painter.state) {
                is AsyncImagePainter.State.Success -> ComposeImage(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )

                is AsyncImagePainter.State.Error -> ImagePlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                    broken = true
                )

                else -> ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** Shimmering block shown while an image is still on its way. */
@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(rememberShimmerBrush()))
}

/** Reserved space for a missing (or failed) image, so layouts never collapse. */
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

/** Mobile Chrome UA: some CDN hosts downgrade or reject the default `; wv` WebView UA. */
internal const val PORTAL_IMAGE_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
