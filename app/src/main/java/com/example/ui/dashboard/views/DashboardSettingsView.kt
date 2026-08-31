package com.example.ui.dashboard.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SiteSettingsRecord
import com.example.data.remote.UserProfile
import com.example.data.remote.UserRole
import com.example.ui.dashboard.components.DashboardImageUploader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSettingsView(
    settings: SiteSettingsRecord,
    onSaveSettings: (SiteSettingsRecord) -> Unit,
    currentUser: UserProfile? = null,
    onUpdateUserCredentials: (String, String?, UserProfile) -> Unit = { _, _, _ -> },
    initialAdminEmail: String = "admin@ningshingche.com",
    modifier: Modifier = Modifier
) {
    var siteTitle by remember { mutableStateOf(settings.siteTitle) }
    var siteDescription by remember { mutableStateOf(settings.siteDescription) }
    var logoUrl by remember { mutableStateOf(settings.logoUrl) }
    var contactEmail by remember { mutableStateOf(settings.contactEmail) }
    var contactPhone by remember { mutableStateOf(settings.contactPhone) }
    var facebookUrl by remember { mutableStateOf(settings.facebookUrl) }
    var youtubeUrl by remember { mutableStateOf(settings.youtubeUrl) }
    var instagramUrl by remember { mutableStateOf(settings.instagramUrl) }

    var heroSliderEnabled by remember { mutableStateOf(settings.heroSliderEnabled) }
    var featuredArticlesEnabled by remember { mutableStateOf(settings.featuredArticlesEnabled) }
    var specialArticlesEnabled by remember { mutableStateOf(settings.specialArticlesEnabled) }
    var allowComments by remember { mutableStateOf(settings.allowComments) }
    var allowUserSubmissions by remember { mutableStateOf(settings.allowUserSubmissions) }

    // User Profile, Credentials & Role Settings State
    var userName by remember { mutableStateOf(currentUser?.fullName ?: "প্রধান সম্পাদক ও প্রশাসক") }
    var userEmail by remember { mutableStateOf(currentUser?.email ?: initialAdminEmail) }
    var userPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf(currentUser?.role ?: UserRole.ADMINISTRATOR) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var isUserSavedNoticeVisible by remember { mutableStateOf(false) }
    var userErrorMessage by remember { mutableStateOf<String?>(null) }

    var isSavedNoticeVisible by remember { mutableStateOf(false) }

    fun saveAllSettings() {
        val updated = settings.copy(
            siteTitle = siteTitle.trim(),
            siteDescription = siteDescription.trim(),
            logoUrl = logoUrl.trim(),
            contactEmail = contactEmail.trim(),
            contactPhone = contactPhone.trim(),
            facebookUrl = facebookUrl.trim(),
            youtubeUrl = youtubeUrl.trim(),
            instagramUrl = instagramUrl.trim(),
            heroSliderEnabled = heroSliderEnabled,
            featuredArticlesEnabled = featuredArticlesEnabled,
            specialArticlesEnabled = specialArticlesEnabled,
            allowComments = allowComments,
            allowUserSubmissions = allowUserSubmissions
        )
        onSaveSettings(updated)
        isSavedNoticeVisible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row with Save Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "সাইট সেটিংস",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "লগইন তথ্য ও সাইট নিয়ন্ত্রণ",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Button(
                onClick = { saveAllSettings() },
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("dashboard_settings_btn_save_top")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("সংরক্ষণ করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Global Success Banner
        AnimatedVisibility(visible = isSavedNoticeVisible) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "সেটিংস সফলভাবে সংরক্ষিত হয়েছে!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Section 1: Account Credentials & Role
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "অ্যাকাউন্ট ও লগইন তথ্য",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Name
                OutlinedTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        isUserSavedNoticeVisible = false
                    },
                    label = { Text("ব্যবহারকারীর পূর্ণ নাম") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_settings_input_username")
                )

                // Login Email
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = {
                        userEmail = it
                        isUserSavedNoticeVisible = false
                        userErrorMessage = null
                    },
                    label = { Text("লগইন ইমেইল ঠিকানা") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_settings_input_login_email")
                )

                // New Password Field
                OutlinedTextField(
                    value = userPassword,
                    onValueChange = {
                        userPassword = it
                        isUserSavedNoticeVisible = false
                        userErrorMessage = null
                    },
                    label = { Text("নতুন পাসওয়ার্ড (পরিবর্তন করতে চাইলে)") },
                    placeholder = { Text("নতুন পাসওয়ার্ড লিখুন") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_settings_input_login_password")
                )

                // Role Dropdown
                ExposedDropdownMenuBox(
                    expanded = roleDropdownExpanded,
                    onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (userRole) {
                            UserRole.ADMINISTRATOR -> "প্রধান প্রশাসক"
                            UserRole.EDITOR -> "সম্পাদক"
                            UserRole.MODERATOR -> "মডারেটর"
                            UserRole.AUTHOR -> "লেখক"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("দায়িত্ব ও ভূমিকা") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("dashboard_settings_dropdown_role")
                    )

                    ExposedDropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false }
                    ) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = when (role) {
                                                UserRole.ADMINISTRATOR -> "প্রধান প্রশাসক"
                                                UserRole.EDITOR -> "সম্পাদক"
                                                UserRole.MODERATOR -> "মডারেটর"
                                                UserRole.AUTHOR -> "লেখক"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = role.description,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                },
                                onClick = {
                                    userRole = role
                                    roleDropdownExpanded = false
                                    isUserSavedNoticeVisible = false
                                }
                            )
                        }
                    }
                }

                // Error Notice
                AnimatedVisibility(visible = userErrorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = userErrorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                            )
                        }
                    }
                }

                // User Saved Notice
                AnimatedVisibility(visible = isUserSavedNoticeVisible) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "লগইন তথ্য সফলভাবে হালনাগাদ হয়েছে!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Save Credentials Button
                Button(
                    onClick = {
                        val trimmedEmail = userEmail.trim()
                        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                            userErrorMessage = "সঠিক ইমেইল ঠিকানা দিন"
                            return@Button
                        }

                        val updatedProfile = (currentUser ?: UserProfile(
                            id = "admin-user",
                            email = trimmedEmail,
                            fullName = userName
                        )).copy(
                            email = trimmedEmail,
                            fullName = userName.trim().ifBlank { "প্রধান সম্পাদক ও প্রশাসক" },
                            role = userRole
                        )

                        val newPass = if (userPassword.isNotBlank()) userPassword.trim() else null
                        onUpdateUserCredentials(trimmedEmail, newPass, updatedProfile)
                        isUserSavedNoticeVisible = true
                        userErrorMessage = null
                        if (newPass != null) {
                            userPassword = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dashboard_settings_btn_save_role")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("অ্যাকাউন্ট তথ্য সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 2: General Info & Logo
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "সাধারণ তথ্য ও লোগো",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                OutlinedTextField(
                    value = siteTitle,
                    onValueChange = {
                        siteTitle = it
                        isSavedNoticeVisible = false
                    },
                    label = { Text("সাইটের নাম ও শিরোনাম") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_site_title")
                )

                OutlinedTextField(
                    value = siteDescription,
                    onValueChange = {
                        siteDescription = it
                        isSavedNoticeVisible = false
                    },
                    label = { Text("সাইটের সংক্ষিপ্ত বিবরণ") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_input_site_desc")
                )

                DashboardImageUploader(
                    imageUrl = logoUrl,
                    onImageUrlChange = {
                        logoUrl = it
                        isSavedNoticeVisible = false
                    },
                    aspectRatio = 3f / 1f,
                    recommendedRatioText = "৩:১ অনুপাত",
                    label = "সাইটের লোগো"
                )
            }
        }

        // Section 3: Contact & Socials
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "যোগাযোগ ও সোশ্যাল মিডিয়া",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = {
                            contactEmail = it
                            isSavedNoticeVisible = false
                        },
                        label = { Text("ইমেইল") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f).testTag("dashboard_input_contact_email")
                    )

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = {
                            contactPhone = it
                            isSavedNoticeVisible = false
                        },
                        label = { Text("ফোন") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f).testTag("dashboard_input_contact_phone")
                    )
                }

                OutlinedTextField(
                    value = facebookUrl,
                    onValueChange = {
                        facebookUrl = it
                        isSavedNoticeVisible = false
                    },
                    label = { Text("ফেসবুক পেইজ লিংক") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = {
                        youtubeUrl = it
                        isSavedNoticeVisible = false
                    },
                    label = { Text("ইউটিউব চ্যানেল লিংক") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instagramUrl,
                    onValueChange = {
                        instagramUrl = it
                        isSavedNoticeVisible = false
                    },
                    label = { Text("ইনস্টাগ্রাম লিংক") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 4: Feature Toggles
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ToggleOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ফিচার ও ডিসপ্লে মডিউল",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                SettingToggleRow(
                    title = "শীর্ষ স্লাইডার",
                    subtitle = "হোমপেজের শীর্ষে নির্বাচিত প্রবন্ধের স্লাইডার প্রদর্শন",
                    checked = heroSliderEnabled,
                    onCheckedChange = {
                        heroSliderEnabled = it
                        isSavedNoticeVisible = false
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingToggleRow(
                    title = "বিশেষ প্রবন্ধ বিভাগ",
                    subtitle = "হোমপেজে বিশিষ্ট নির্বাচিত প্রবন্ধের গ্রিড",
                    checked = featuredArticlesEnabled,
                    onCheckedChange = {
                        featuredArticlesEnabled = it
                        isSavedNoticeVisible = false
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingToggleRow(
                    title = "পাঠক রচনা জমা ফরম",
                    subtitle = "পাঠকদের কাছ থেকে লেখা গ্রহণ সক্রিয়করণ",
                    checked = allowUserSubmissions,
                    onCheckedChange = {
                        allowUserSubmissions = it
                        isSavedNoticeVisible = false
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingToggleRow(
                    title = "পাঠক মন্তব্য ব্যবস্থা",
                    subtitle = "প্রবন্ধসমূহে পাঠকদের মন্তব্য গ্রহণের সুবিধা",
                    checked = allowComments,
                    onCheckedChange = {
                        allowComments = it
                        isSavedNoticeVisible = false
                    }
                )
            }
        }

        // Bottom Save Button
        Button(
            onClick = { saveAllSettings() },
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("dashboard_settings_btn_save_bottom")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("সকল সেটিংস সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
