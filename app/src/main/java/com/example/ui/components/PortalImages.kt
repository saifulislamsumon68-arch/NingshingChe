package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.repository.NinghsingCheContentData

@Composable
fun PortalAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val cleaned = remember(url) { normalizePortalImageUrl(url) }
    val request = remember(cleaned) {
        ImageRequest.Builder(context)
            .data(cleaned.ifBlank { NinghsingCheContentData.APP_LOGO_URL })
            .crossfade(true)
            .addHeader("Referer", "https://ningshingche.com/")
            .addHeader("User-Agent", PORTAL_IMAGE_UA)
            .addHeader("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            .error(R.drawable.ic_ningshingche_logo)
            .fallback(R.drawable.ic_ningshingche_logo)
            .placeholder(R.drawable.ic_ningshingche_logo)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

fun normalizePortalImageUrl(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return raw.trim()
        .replace(" ", "%20")
        .replace("hyphenhyphen", "-")
}

private const val PORTAL_IMAGE_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
