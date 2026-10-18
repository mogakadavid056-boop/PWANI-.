package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPrimary

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SupportFooter(
  showHelpBanner: Boolean = false,
  modifier: Modifier = Modifier
) {
  // Hide footer when the software keyboard is active so it never obstructs inputs
  if (WindowInsets.isImeVisible) return

  val context = LocalContext.current

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF0B2A1E))
      .testTag("support_footer")
  ) {
    // Thin gold top line
    HorizontalDivider(
      thickness = 1.dp,
      color = GoldPrimary.copy(alpha = 0.6f)
    )

    if (showHelpBanner) {
      Text(
        text = SupportConfig.AUTH_HELP_TEXT,
        fontSize = 11.sp,
        color = Color(0xFFE4E4E7),
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 5.dp, bottom = 2.dp, start = 12.dp, end = 12.dp)
          .testTag("support_auth_help_text")
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(40.dp)
        .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(GoldPrimary, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
          text = "Support Desk",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = GoldPrimary
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        // Email button
        IconButton(
          onClick = { openEmail(context) },
          modifier = Modifier.size(36.dp).testTag("support_email_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Email Support",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }

        // WhatsApp button
        IconButton(
          onClick = { openWhatsApp(context) },
          modifier = Modifier.size(36.dp).testTag("support_whatsapp_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "WhatsApp Support",
            tint = Color(0xFF25D366),
            modifier = Modifier.size(18.dp)
          )
        }

        // Website button
        IconButton(
          onClick = { openWebsite(context) },
          modifier = Modifier.size(36.dp).testTag("support_website_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Language,
            contentDescription = "Support Website",
            tint = GoldPrimary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

private fun openEmail(context: Context) {
  try {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("mailto:${SupportConfig.EMAIL}?subject=${Uri.encode(SupportConfig.EMAIL_SUBJECT)}")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  } catch (_: Exception) {
    try {
      val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(SupportConfig.EMAIL_URI)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(fallback)
    } catch (_: Exception) {}
  }
}

private fun openWhatsApp(context: Context) {
  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SupportConfig.WHATSAPP_URL)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  } catch (_: Exception) {}
}

private fun openWebsite(context: Context) {
  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SupportConfig.WEBSITE_URL)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  } catch (_: Exception) {}
}
