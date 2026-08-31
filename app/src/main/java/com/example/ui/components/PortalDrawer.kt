package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.PortalNavItem
import com.example.ui.navigation.PortalNavigation
import com.example.ui.navigation.Screen
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalMaroon
import com.example.ui.theme.PortalSaffron

@Composable
fun PortalDrawerContent(
    currentRoute: String,
    isDark: Boolean,
    onNavigate: (String) -> Unit,
    onCategory: (String) -> Unit,
    onYear: (Int) -> Unit,
    onExternal: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onShareApp: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF111827) else PortalMaroon)
                    .padding(horizontal = 18.dp, vertical = 22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NingshingCheBrandLogo(size = 48.dp)
                    Column {
                        Text("নিংশিং চে", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 22.sp)
                        Text("বিষ্ণুপ্রিয়া মণিপুরি তথ্যকোষ", fontFamily = Kalpurush, color = PortalSaffron, fontSize = 12.sp)
                    }
                }
            }

            SectionLabel("প্রধান পাতা")
            DrawerRow("ঘর", Icons.Default.Home, currentRoute == Screen.Home.route) {
                onCloseDrawer(); onNavigate(Screen.Home.route)
            }
            DrawerRow("ফিচার্ড", Icons.Default.Star, currentRoute == Screen.Featured.route) {
                onCloseDrawer(); onNavigate(Screen.Featured.route)
            }
            DrawerRow("অন্বেষণ", Icons.Default.Explore, currentRoute == Screen.Explore.route) {
                onCloseDrawer(); onNavigate(Screen.Explore.route)
            }
            DrawerRow("PDF আর্কাইভ", Icons.Default.PictureAsPdf, currentRoute == Screen.PdfArchive.route) {
                onCloseDrawer(); onNavigate(Screen.PdfArchive.route)
            }
            DrawerRow("অনুসন্ধান", Icons.Default.Search, currentRoute == Screen.Search.route) {
                onCloseDrawer(); onNavigate(Screen.Search.route)
            }
            DrawerRow("সংরক্ষিত", Icons.Default.Bookmark, currentRoute == Screen.Bookmarks.route) {
                onCloseDrawer(); onNavigate(Screen.Bookmarks.route)
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            SectionLabel("অনলাইন চে — বার্ষিক সংখ্যা")
            PortalNavigation.years.forEach { item ->
                DrawerRow(item.label, Icons.Default.CalendarMonth, false) {
                    onCloseDrawer()
                    item.year?.let(onYear)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            SectionLabel("বিভাগসমূহ")
            PortalNavigation.categories.forEach { item ->
                DrawerRow(item.label, Icons.Default.MenuBook, currentRoute.contains(item.categorySlug.orEmpty())) {
                    onCloseDrawer()
                    item.categorySlug?.let(onCategory)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            SectionLabel("পোর্টাল")
            DrawerRow("আমার সম্পর্কে", Icons.Default.Info, currentRoute == Screen.About.route) {
                onCloseDrawer(); onNavigate(Screen.About.route)
            }
            DrawerRow("লেখক", Icons.Default.People, currentRoute == Screen.AuthorsDirectory.route) {
                onCloseDrawer(); onNavigate(Screen.AuthorsDirectory.route)
            }
            DrawerRow("সামাজিক কার্যকলাপ", Icons.Default.VolunteerActivism, currentRoute == Screen.SocialActivities.route) {
                onCloseDrawer(); onNavigate(Screen.SocialActivities.route)
            }
            DrawerRow("লেখা জমাদান", Icons.Default.Language, false) {
                onCloseDrawer(); onExternal("https://ningshingche.com/blog_submission")
            }
            DrawerRow("ningshingche.com", Icons.Default.Language, false) {
                onCloseDrawer(); onExternal("https://ningshingche.com")
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            SectionLabel("অ্যাপ")
            DrawerRow("AI সহকারী", Icons.Default.AutoAwesome, currentRoute == Screen.AiAssistant.route) {
                onCloseDrawer(); onNavigate(Screen.AiAssistant.route)
            }
            DrawerRow("সেটিংস", Icons.Default.Settings, currentRoute == Screen.Settings.route) {
                onCloseDrawer(); onNavigate(Screen.Settings.route)
            }
            DrawerRow("ড্যাশবোর্ড (CMS)", Icons.Default.Dashboard, currentRoute == Screen.Dashboard.route) {
                onCloseDrawer(); onNavigate(Screen.Dashboard.route)
            }
            DrawerRow(
                if (isDark) "লাইট থিম" else "ডার্ক থিম",
                if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                false
            ) {
                onToggleTheme()
            }
            DrawerRow("অ্যাপ শেয়ার করুন", Icons.Default.Share, false) {
                onCloseDrawer(); onShareApp()
            }

            Text(
                text = "কালপুরুষ ফন্ট • নিংশিং চে পোর্টাল",
                fontFamily = Kalpurush,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = Kalpurush,
        color = PortalSaffron,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) PortalSaffron else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Text(
                text = label,
                fontFamily = Kalpurush,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp
            )
        },
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .testTag("drawer_${label}")
    )
}
