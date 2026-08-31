package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.4f

    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF1F2937),
            Color(0xFF374151),
            Color(0xFF1F2937)
        )
    } else {
        listOf(
            Color(0xFFE5E7EB),
            Color(0xFFF3F4F6),
            Color(0xFFE5E7EB)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    return this
        .clip(shape)
        .background(brush)
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier = modifier.shimmerEffect(shape = shape))
}

@Composable
fun HeroCarouselSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShimmerBox(modifier = Modifier.size(width = 85.dp, height = 24.dp), shape = RoundedCornerShape(6.dp))
                    ShimmerBox(modifier = Modifier.size(width = 50.dp, height = 20.dp), shape = RoundedCornerShape(10.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(22.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f).height(22.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp))
                }
            }
        }

        // Indicator dots skeleton (3 carousel items)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(width = 24.dp, height = 8.dp), shape = RoundedCornerShape(4.dp))
            ShimmerBox(modifier = Modifier.size(8.dp), shape = CircleShape)
            ShimmerBox(modifier = Modifier.size(8.dp), shape = CircleShape)
        }
    }
}

@Composable
fun HeroCardSkeleton(modifier: Modifier = Modifier) {
    HeroCarouselSkeleton(modifier = modifier)
}

@Composable
fun ArticleCardSkeleton(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShimmerBox(modifier = Modifier.size(width = 65.dp, height = 18.dp), shape = RoundedCornerShape(6.dp))
                    ShimmerBox(modifier = Modifier.size(width = 45.dp, height = 14.dp), shape = RoundedCornerShape(4.dp))
                }

                ShimmerBox(modifier = Modifier.fillMaxWidth().height(18.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.75f).height(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerBox(modifier = Modifier.size(20.dp), shape = CircleShape)
                    ShimmerBox(modifier = Modifier.size(width = 90.dp, height = 12.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryChipSkeleton() {
    ShimmerBox(
        modifier = Modifier
            .size(width = 110.dp, height = 40.dp),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun PdfCardSkeleton(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width(85.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBox(modifier = Modifier.size(width = 90.dp, height = 18.dp), shape = RoundedCornerShape(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(20.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(20.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(modifier = Modifier.size(width = 80.dp, height = 14.dp))
                    ShimmerBox(modifier = Modifier.size(width = 75.dp, height = 32.dp), shape = RoundedCornerShape(16.dp))
                }
            }
        }
    }
}

@Composable
fun HomeSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        HeroCarouselSkeleton()

        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(5) {
                CategoryChipSkeleton()
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 22.dp))
            repeat(4) {
                ArticleCardSkeleton()
            }
        }
    }
}

@Composable
fun ExploreSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(16.dp)
            )
            ShimmerBox(
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(16.dp)
            )
            ShimmerBox(
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }

        repeat(3) {
            ArticleCardSkeleton()
        }
    }
}

@Composable
fun PdfArchiveSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(4) {
                CategoryChipSkeleton()
            }
        }

        repeat(4) {
            PdfCardSkeleton()
        }
    }
}

@Composable
fun BookmarksSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(4) {
                CategoryChipSkeleton()
            }
        }

        repeat(4) {
            ArticleCardSkeleton()
        }
    }
}

@Composable
fun HistorySkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(8.dp))

        repeat(5) {
            ArticleCardSkeleton()
        }
    }
}

@Composable
fun SearchSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ShimmerBox(modifier = Modifier.size(width = 80.dp, height = 32.dp), shape = RoundedCornerShape(16.dp))
            ShimmerBox(modifier = Modifier.size(width = 100.dp, height = 32.dp), shape = RoundedCornerShape(16.dp))
            ShimmerBox(modifier = Modifier.size(width = 90.dp, height = 32.dp), shape = RoundedCornerShape(16.dp))
        }

        repeat(4) {
            ArticleCardSkeleton()
        }
    }
}

@Composable
fun DetailListSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            shape = RoundedCornerShape(16.dp)
        )

        ShimmerBox(modifier = Modifier.size(width = 150.dp, height = 20.dp))

        repeat(4) {
            ArticleCardSkeleton()
        }
    }
}

@Composable
fun AiAssistantSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(20.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ShimmerBox(modifier = Modifier.size(width = 110.dp, height = 36.dp), shape = RoundedCornerShape(18.dp))
            ShimmerBox(modifier = Modifier.size(width = 120.dp, height = 36.dp), shape = RoundedCornerShape(18.dp))
            ShimmerBox(modifier = Modifier.size(width = 100.dp, height = 36.dp), shape = RoundedCornerShape(18.dp))
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ShimmerBox(modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(28.dp))
    }
}

@Composable
fun SettingsSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ShimmerBox(modifier = Modifier.size(width = 120.dp, height = 20.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(44.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(44.dp))
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ShimmerBox(modifier = Modifier.size(width = 140.dp, height = 20.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(36.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(36.dp))
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 20.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp))
            }
        }
    }
}

@Composable
fun PdfViewerSkeletonLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(10.dp))

        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(0.72f),
            shape = RoundedCornerShape(16.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(40.dp), shape = CircleShape)
            ShimmerBox(modifier = Modifier.size(width = 100.dp, height = 24.dp))
            ShimmerBox(modifier = Modifier.size(40.dp), shape = CircleShape)
        }
    }
}

@Composable
fun ArticleReaderSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(modifier = Modifier.size(width = 90.dp, height = 24.dp), shape = RoundedCornerShape(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(32.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(modifier = Modifier.size(44.dp), shape = CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShimmerBox(modifier = Modifier.size(width = 140.dp, height = 16.dp))
                ShimmerBox(modifier = Modifier.size(width = 90.dp, height = 12.dp))
            }
        }

        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp)
        )

        repeat(6) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp))
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
    }
}
