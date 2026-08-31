package com.example.ui.reader

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.portal.stripHtml
import com.example.ui.editorial.LocalEditorialTokens
import com.example.ui.theme.Kalpurush
import java.util.Locale

/**
 * State for Voice Synthesis TTS Player.
 */
enum class TtsPlayState {
    IDLE,
    INITIALIZING,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR
}

/**
 * Helper class managing Android TextToSpeech engine.
 */
class ArticleTtsController(
    private val context: Context,
    private val onStateChange: (TtsPlayState) -> Unit,
    private val onError: (String) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var textChunks: List<String> = emptyList()
    private var currentChunkIndex = 0
    private var speechRate = 1.0f

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                val bnLocale = Locale("bn", "BD")
                val langResult = tts?.setLanguage(bnLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val genericBn = Locale("bn")
                    val resultGeneric = tts?.setLanguage(genericBn)
                    if (resultGeneric == TextToSpeech.LANG_MISSING_DATA || resultGeneric == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.getDefault())
                    }
                }
                tts?.setSpeechRate(speechRate)
                setupListener()
            } else {
                onStateChange(TtsPlayState.ERROR)
                onError("ভয়েস ইঞ্জিন প্রস্তুত করা যায়নি।")
            }
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStateChange(TtsPlayState.PLAYING)
            }

            override fun onDone(utteranceId: String?) {
                currentChunkIndex++
                if (currentChunkIndex < textChunks.size) {
                    speakCurrentChunk()
                } else {
                    onStateChange(TtsPlayState.COMPLETED)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onStateChange(TtsPlayState.ERROR)
                onError("পড়ে শোনানোর সময় ত্রুটি হয়েছে।")
            }
        })
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun start(title: String, htmlContent: String) {
        val cleanBody = stripHtml(htmlContent)
        val fullText = "$title. $cleanBody"

        // Break into sentences/chunks for smooth playback and responsiveness
        textChunks = fullText.split(Regex("([।?!\\n\\.]+)"))
            .map { it.trim() }
            .filter { it.length > 1 }

        if (textChunks.isEmpty()) {
            onError("পড়ার মতো কোনো লেখা পাওয়া যায়নি।")
            return
        }

        currentChunkIndex = 0
        if (!isInitialized) {
            onStateChange(TtsPlayState.INITIALIZING)
        } else {
            speakCurrentChunk()
        }
    }

    private fun speakCurrentChunk() {
        if (currentChunkIndex >= textChunks.size) {
            onStateChange(TtsPlayState.COMPLETED)
            return
        }
        val chunk = textChunks[currentChunkIndex]
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "chunk_$currentChunkIndex")
        }
        tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, "chunk_$currentChunkIndex")
        onStateChange(TtsPlayState.PLAYING)
    }

    fun pause() {
        tts?.stop()
        onStateChange(TtsPlayState.PAUSED)
    }

    fun resume() {
        if (currentChunkIndex < textChunks.size) {
            speakCurrentChunk()
        } else {
            currentChunkIndex = 0
            speakCurrentChunk()
        }
    }

    fun stop() {
        tts?.stop()
        currentChunkIndex = 0
        onStateChange(TtsPlayState.IDLE)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

/**
 * Floating/Embedded Voice Synthesis Player Bar on the Article Reader.
 */
@Composable
fun ArticleTtsPlayerBar(
    title: String,
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalEditorialTokens.current

    var playState by remember { mutableStateOf(TtsPlayState.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var speechSpeed by remember { mutableFloatStateOf(1.0f) }
    var speedMenuExpanded by remember { mutableStateOf(false) }

    val controller = remember {
        ArticleTtsController(
            context = context,
            onStateChange = { newState -> playState = newState },
            onError = { msg -> errorMessage = msg }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.shutdown()
        }
    }

    LaunchedEffect(speechSpeed) {
        controller.setSpeechRate(speechSpeed)
    }

    // Modern TTS Player Surface
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tokens.surfaceSunken,
        border = BorderStroke(1.dp, tokens.accent.copy(alpha = 0.35f)),
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(tokens.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playState == TtsPlayState.PLAYING) Icons.Default.GraphicEq else Icons.Default.RecordVoiceOver,
                            contentDescription = "ভয়েস রিডার",
                            tint = tokens.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ভয়েস সিন্থেসিস (AI পাঠ)",
                            fontFamily = Kalpurush,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (playState) {
                                TtsPlayState.IDLE -> "সম্পূর্ণ প্রবন্ধটি শুনুন"
                                TtsPlayState.INITIALIZING -> "ভয়েস ইঞ্জিন প্রস্তুত হচ্ছে..."
                                TtsPlayState.PLAYING -> "পড়ে শোনানো হচ্ছে..."
                                TtsPlayState.PAUSED -> "পাঠ স্থগিত আছে"
                                TtsPlayState.COMPLETED -> "পাঠ সম্পন্ন হয়েছে"
                                TtsPlayState.ERROR -> errorMessage ?: "ত্রুটি হয়েছে"
                            },
                            fontFamily = Kalpurush,
                            fontSize = 12.sp,
                            color = if (playState == TtsPlayState.ERROR) tokens.accent else tokens.inkMuted
                        )
                    }
                }

                // Action controls: Speed + Play/Pause/Stop
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Speed selector
                    Box {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = tokens.surfaceSunken,
                            border = BorderStroke(1.dp, tokens.rule),
                            modifier = Modifier.clickable { speedMenuExpanded = true }
                        ) {
                            Text(
                                text = "${speechSpeed}x",
                                fontFamily = Kalpurush,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = tokens.accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false }
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${speed}x গতি",
                                            fontFamily = Kalpurush,
                                            fontWeight = if (speed == speechSpeed) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        speechSpeed = speed
                                        speedMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Main Play/Pause Button
                    when (playState) {
                        TtsPlayState.INITIALIZING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                strokeWidth = 2.5.dp,
                                color = tokens.accent
                            )
                        }
                        TtsPlayState.PLAYING -> {
                            FilledIconButton(
                                onClick = { controller.pause() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = tokens.accent,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "স্থগিত করুন")
                            }
                            IconButton(
                                onClick = { controller.stop() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "বন্ধ করুন", tint = tokens.inkMuted)
                            }
                        }
                        TtsPlayState.PAUSED -> {
                            FilledIconButton(
                                onClick = { controller.resume() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = tokens.accent,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "পুনরায় চালান")
                            }
                            IconButton(
                                onClick = { controller.stop() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "বন্ধ করুন", tint = tokens.inkMuted)
                            }
                        }
                        TtsPlayState.IDLE, TtsPlayState.COMPLETED, TtsPlayState.ERROR -> {
                            FilledIconButton(
                                onClick = {
                                    errorMessage = null
                                    controller.start(title, htmlContent)
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = tokens.accent,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "শুনুন")
                            }
                        }
                    }
                }
            }
        }
    }
}
