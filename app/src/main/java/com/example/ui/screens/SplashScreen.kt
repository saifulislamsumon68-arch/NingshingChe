package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NingshingCheBrandLogo
import com.example.ui.editorial.EditorialPalette
import com.example.ui.editorial.EditorialType
import com.example.ui.editorial.LocalEditorialTokens
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalMaroon
import com.example.ui.theme.PortalSaffron
import kotlinx.coroutines.delay

/**
 * Animated Brand Logo Splash Screen on app launch.
 *
 * Displays the high-resolution NingshingChe emblem with radial pulsing glow,
 * typography in Kalpurush / Noto Serif, culture tagline, and a smooth auto-transition
 * into the main portal reader.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splash_fade"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_loop")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        // 1.5 seconds display time for smooth branding impression
        delay(1500L)
        onSplashComplete()
    }

    val isDark = LocalEditorialTokens.current.isDark
    val bgGradient = if (isDark) {
        listOf(Color(0xFF14100E), Color(0xFF1E1815), Color(0xFF0F0C0A))
    } else {
        listOf(Color(0xFFFFFBF5), Color(0xFFFBF4E8), Color(0xFFF4EBD9))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
            .clickable { onSplashComplete() }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background concentric culture rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val maxRadius = size.minDimension * 0.45f
            drawCircle(
                color = (if (isDark) Color(0xFFF0A94B) else PortalMaroon).copy(alpha = 0.04f),
                radius = maxRadius * 1.3f,
                center = center
            )
            drawCircle(
                color = (if (isDark) Color(0xFFF0A94B) else PortalMaroon).copy(alpha = 0.06f),
                radius = maxRadius * 0.9f,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .alpha(alphaAnim)
                .scale(scaleAnim)
        ) {
            // Pulsing emblem container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Pulsing outer halo ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isDark) Color(0xFFF0A94B) else PortalSaffron,
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Logo container card
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().padding(10.dp)
                    ) {
                        NingshingCheBrandLogo(size = 90.dp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // App Title
            Text(
                text = "নিংশিং চে",
                fontFamily = Kalpurush,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                letterSpacing = (-0.5).sp,
                color = if (isDark) Color(0xFFF7F2EA) else PortalMaroon,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "বিষ্ণুপ্রিয়া মণিপুরি সাহিত্য ও সংস্কৃতি পোর্টাল",
                fontFamily = Kalpurush,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (isDark) Color(0xFFF0A94B) else PortalSaffron,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // Tagline chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = (if (isDark) Color(0xFF33231D) else EditorialPalette.MaroonSoft).copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFF0A94B) else PortalMaroon,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "জ্ঞান, ইতিহাস ও সাহিত্যের মুক্ত তথ্যকোষ",
                        fontFamily = Kalpurush,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFF7F2EA) else PortalMaroon,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Loading progress bar with sleek styling
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color(0xFF2B2521) else Color(0xFFE2D6C6))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDark) Color(0xFFF0A94B) else PortalMaroon,
                    trackColor = Color.Transparent
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "সংস্কৃতির আলোয় পথচলা…",
                fontFamily = Kalpurush,
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF9C9186) else EditorialPalette.InkMuted
            )
        }

        // Bottom footer credits
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .alpha(alphaAnim)
        ) {
            Text(
                text = "ningshingche.com • সংস্করণ ১.০",
                fontFamily = Kalpurush,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF7A6E62) else Color(0xFFA39587)
            )
        }
    }
}
