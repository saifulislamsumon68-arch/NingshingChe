package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF1D9BF0),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "যাচাইকৃত লেখক",
                tint = Color.White,
                modifier = Modifier.size((size.value * 0.72f).dp)
            )
        }
    }
}
