package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalDarkBg
import com.example.ui.theme.PortalMaroon
import com.example.ui.theme.PortalSaffron

@Composable
fun PortalTopBar(
    isDark: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAiClick: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val barColor by animateColorAsState(
        targetValue = if (isDark) PortalDarkBg else PortalMaroon,
        animationSpec = tween(320),
        label = "portal_bar"
    )
    val iconTint = Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(barColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .testTag("hamburger_menu_button")
            ) {
                Icon(Icons.Default.Menu, contentDescription = "মেনু", tint = iconTint)
            }

            NingshingCheBrandLogo(size = 38.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "নিংশিং চে",
                    fontFamily = Kalpurush,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 24.sp
                )
                Text(
                    text = "বিষ্ণুপ্রিয়া মণিপুরি তথ্যকোষ",
                    fontFamily = Kalpurush,
                    color = PortalSaffron,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDark) "লাইট থিম" else "ডার্ক থিম",
                    tint = PortalSaffron
                )
            }
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("search_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = "অনুসন্ধান", tint = iconTint)
            }
            IconButton(
                onClick = onAiClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, PortalSaffron, CircleShape)
                    .testTag("ai_assistant_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = PortalSaffron)
            }
        }
    }
}

@Composable
fun StickySubNav(
    items: List<Pair<String, () -> Unit>>,
    isDark: Boolean
) {
    val bg by animateColorAsState(
        if (isDark) Color(0xFF1F2937) else Color.White,
        tween(320),
        label = "subnav"
    )
    val fg by animateColorAsState(
        if (isDark) Color(0xFFF9FAFB) else Color(0xFF4B2E2B),
        tween(320),
        label = "subnav_fg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, onClick) ->
            Text(
                text = label,
                fontFamily = Kalpurush,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                modifier = Modifier.clickable(onClick = onClick)
            )
        }
    }
}
