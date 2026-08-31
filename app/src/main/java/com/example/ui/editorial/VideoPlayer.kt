package com.example.ui.editorial

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.portal.VideoItem
import com.example.ui.theme.Kalpurush
import java.net.URLEncoder

// ---------------------------------------------------------------------------
// Embed resolution
// ---------------------------------------------------------------------------

/** Matches the video id in every YouTube URL shape we store: watch?v=, /shorts/, /embed/, youtu.be/, /live/. */
private val YOUTUBE_ID = Regex("""(?:v=|/shorts/|/embed/|/live/|youtu\.be/)([A-Za-z0-9_-]{11})""")

/**
 * Builds the embeddable player URL for a stored video.
 *
 * YouTube links are rewritten to the `/embed/<id>` iframe endpoint, which is the only
 * form that plays inline in a WebView. Facebook has no direct video file endpoint, so its
 * official `plugins/video.php` embed is used instead.
 *
 * Note: Facebook deliberately degrades inside embedded browsers and may show a login
 * wall. That is a Facebook policy, not a bug here — hence the "open in browser" affordance
 * in [VideoPlayerDialog].
 */
internal fun embedUrlFor(video: VideoItem): String {
    val isYouTube = video.platform.contains("youtube", ignoreCase = true) ||
        video.url.contains("youtube", ignoreCase = true) ||
        video.url.contains("youtu.be", ignoreCase = true)

    if (isYouTube) {
        val id = YOUTUBE_ID.find(video.url)?.groupValues?.getOrNull(1)
        if (id != null) {
            // playsinline=1 keeps playback in the page instead of handing off to the
            // YouTube app; fs=1 keeps the fullscreen control usable.
            return "https://www.youtube.com/embed/$id" +
                "?autoplay=1&rel=0&playsinline=1&modestbranding=1&fs=1"
        }
    }

    val encoded = URLEncoder.encode(video.url, "UTF-8")
    return "https://www.facebook.com/plugins/video.php" +
        "?href=$encoded&show_text=false&autoplay=true&mute=0&width=560"
}

private fun playerHtml(src: String): String = """
    <!DOCTYPE html>
    <html>
    <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <style>
      html, body { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
      #player { position:absolute; top:0; left:0; width:100%; height:100%; border:0; }
    </style>
    </head>
    <body>
    <iframe id="player"
        src="$src"
        frameborder="0"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
        allowfullscreen></iframe>
    </body>
    </html>
""".trimIndent()

/** A Chrome UA without the `; wv` marker, which YouTube and Facebook treat as an embedded browser. */
private const val PLAYER_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

// ---------------------------------------------------------------------------
// Player dialog
// ---------------------------------------------------------------------------

/**
 * Full-screen in-app video player.
 *
 * Plays YouTube via its official iframe embed and Facebook via the video plugin, both
 * inside a WebView. The WebView is paused with the hosting lifecycle and destroyed on
 * release, so closing the dialog stops audio immediately.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onOpenExternal: (String) -> Unit
) {
    val html = remember(video.id) { playerHtml(embedUrlFor(video)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var webView: WebView? by remember { mutableStateOf(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(AndroidColor.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            // Required for the autoplay=1 query parameter to take effect.
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = false
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            userAgentString = PLAYER_UA
                        }
                        webViewClient = WebViewClient()
                        // Needed so the iframe can go fullscreen.
                        webChromeClient = WebChromeClient()
                        loadDataWithBaseURL("https://ningshingche.com/", html, "text/html", "utf-8", null)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { view ->
                    view.stopLoading()
                    view.webChromeClient = null
                    view.destroy()
                }
            )

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                        Lifecycle.Event.ON_RESUME -> webView?.onResume()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Header: title with a close button and a manual escape hatch to the browser.
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
                }
                Text(
                    text = video.title,
                    fontFamily = Kalpurush,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = { onOpenExternal(video.url) },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Icon(
                        Icons.Filled.OpenInNew,
                        contentDescription = "ব্রাউজারে খুলুন",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }
        }
    }
}
