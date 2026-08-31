package com.example.ui.editorial

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.portal.SiteSettings
import com.example.ui.theme.Kalpurush

/**
 * Footer details, mirroring the footer of https://ningshingche.com.
 *
 * The `settings` row in Supabase currently stores empty contact and social columns, so
 * every value here falls back to what the live site publishes. [resolve] prefers the API
 * the moment an editor fills those columns in, without needing a code change.
 */
object SiteContact {
    const val TAGLINE = "বিষ্ণুপ্রিয়া মণিপুরি তথ্যকোষ"
    const val ADDRESS = "তিলকপুর, কমলগঞ্জ, মৌলভীবাজার, সিলেট"
    const val MAPS_URL = "https://maps.app.goo.gl/1SmaUj5kGPYgP7gJA"
    const val PHONE = "+880 9638-781890"
    const val PHONE_URI = "tel:+8809638781890"
    const val EMAIL = "ningshingche@gmail.com"
    const val EMAIL_URI = "mailto:ningshingche@gmail.com"
    const val WEBSITE = "https://ningshingche.com"
    const val SUBMIT_URL = "https://ningshingche.com/blog_submission"
    const val PRIVACY_URL = "https://ningshingche.com/"
    const val TERMS_URL = "https://ningshingche.com/"
    const val SITEMAP_URL = "https://ningshingche.com/"
    const val FACEBOOK_URL = "https://www.facebook.com/ningshingche/"
    const val WHATSAPP_URL = "https://whatsapp.com/channel/0029Vab82QgKwqSLmgh9Ma2N"
    const val YOUTUBE_URL = "https://www.youtube.com/@ningshingche"
    const val PLAY_STORE_URL =
        "https://play.google.com/store/apps/details?id=com.shakilsoftltd.ningshingche"
    const val DEVELOPER_URL = "https://kehem.com/"
    const val DEVELOPER_NAME = "কেহেম আইটি"

    /** API value first, published site value second. */
    fun resolve(settings: SiteSettings) = Resolved(
        email = settings.contactEmail.ifBlank { EMAIL },
        emailUri = "mailto:${settings.contactEmail.ifBlank { EMAIL }}",
        facebook = settings.facebookUrl.ifBlank { FACEBOOK_URL },
        youtube = settings.youtubeUrl.ifBlank { YOUTUBE_URL },
        instagram = settings.instagramUrl
    )

    data class Resolved(
        val email: String,
        val emailUri: String,
        val facebook: String,
        val youtube: String,
        val instagram: String
    )
}

// Matches the dark footer band on ningshingche.com in both light and dark app themes.
private val FooterBg = Color(0xFF1A1512)
private val FooterText = Color(0xFFF3ECE2)
private val FooterMuted = Color(0xFFB9A795)
private val FooterAccent = Color(0xFFD97706)

/**
 * The app footer: brand block, contact details, quick links, social channels, the
 * Play Store download and the development credit.
 *
 * @param onNavigate handles destinations that exist inside the app (About).
 * @param onOpenLink handles everything external — `http`, `tel:` and `mailto:` alike.
 */
@Composable
fun EditorialFooter(
    settings: SiteSettings,
    onNavigate: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val info = SiteContact.resolve(settings)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FooterBg)
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.xl)
    ) {
        // --- Brand --------------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_ningshingche_logo),
                contentDescription = settings.title,
                modifier = Modifier.height(44.dp)
            )
            Spacer(Modifier.width(EditorialSpace.md))
            Column {
                Text(
                    text = settings.title,
                    fontFamily = Kalpurush,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FooterText
                )
                Text(
                    text = SiteContact.TAGLINE,
                    fontFamily = Kalpurush,
                    fontSize = 13.sp,
                    color = FooterAccent
                )
            }
        }

        Spacer(Modifier.height(EditorialSpace.lg))

        // --- Contact ------------------------------------------------------
        ContactRow(
            text = SiteContact.ADDRESS,
            onClick = { onOpenLink(SiteContact.MAPS_URL) }
        )
        ContactRow(
            text = SiteContact.PHONE,
            onClick = { onOpenLink(SiteContact.PHONE_URI) }
        )
        ContactRow(
            text = info.email,
            onClick = { onOpenLink(info.emailUri) }
        )

        Spacer(Modifier.height(EditorialSpace.lg))
        FooterDivider()

        // --- Quick links --------------------------------------------------
        FooterHeading("দ্রুত লিংকসমূহ")
        Spacer(Modifier.height(EditorialSpace.sm))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                FooterLink("আমাদের সম্পর্কে") { onNavigate("about") }
                FooterLink("লেখা জমাদান") { onOpenLink(SiteContact.SUBMIT_URL) }
                FooterLink("গোপনীয়তা নীতি") { onOpenLink(SiteContact.PRIVACY_URL) }
            }
            Spacer(Modifier.width(EditorialSpace.md))
            Column(modifier = Modifier.weight(1f)) {
                FooterLink("শর্তাবলী") { onOpenLink(SiteContact.TERMS_URL) }
                FooterLink("যোগাযোগ করুন") { onOpenLink(info.emailUri) }
                FooterLink("সাইটম্যাপ") { onOpenLink(SiteContact.SITEMAP_URL) }
            }
        }

        Spacer(Modifier.height(EditorialSpace.lg))
        FooterDivider()

        // --- Social -------------------------------------------------------
        FooterHeading("আমাদের অনুসরণ করুন")
        Spacer(Modifier.height(EditorialSpace.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)) {
            SocialButton(R.drawable.ic_social_facebook, "Facebook") {
                onOpenLink(info.facebook)
            }
            SocialButton(R.drawable.ic_social_whatsapp, "WhatsApp") {
                onOpenLink(SiteContact.WHATSAPP_URL)
            }
            SocialButton(R.drawable.ic_social_youtube, "YouTube") {
                onOpenLink(info.youtube)
            }
        }

        Spacer(Modifier.height(EditorialSpace.lg))
        FooterDivider()

        // --- Download -----------------------------------------------------
        FooterHeading("সর্বশেষ আপডেট পেতে আমাদের অ্যাপটি ডাউনলোড করুন")
        Spacer(Modifier.height(EditorialSpace.sm))
        Surface(
            onClick = { onOpenLink(SiteContact.PLAY_STORE_URL) },
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF7A2E1E)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_store_play),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Play Store",
                    fontFamily = Kalpurush,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(EditorialSpace.lg))
        FooterDivider()

        // --- Credit -------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "© ২০২৫ ${settings.title} | সাইট উন্নয়ন ",
                fontFamily = Kalpurush,
                fontSize = 12.sp,
                color = FooterMuted,
                textAlign = TextAlign.Center
            )
            Text(
                text = SiteContact.DEVELOPER_NAME,
                fontFamily = Kalpurush,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FooterAccent,
                modifier = Modifier.clickable { onOpenLink(SiteContact.DEVELOPER_URL) }
            )
        }
    }
}

@Composable
private fun ContactRow(
    text: String,
    onClick: (() -> Unit)?,
    icon: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = FooterAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontFamily = Kalpurush,
            fontSize = 13.sp,
            color = FooterMuted
        )
    }
}

@Composable
private fun FooterHeading(text: String) {
    Text(
        text = text,
        fontFamily = Kalpurush,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = FooterText
    )
}

@Composable
private fun FooterLink(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = FooterAccent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontFamily = Kalpurush,
            fontSize = 13.sp,
            color = FooterMuted
        )
    }
}

@Composable
private fun SocialButton(icon: Int, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF7A2E1E),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(icon),
                contentDescription = label,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun FooterDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF3A2F29))
    )
    Spacer(Modifier.height(EditorialSpace.lg))
}
