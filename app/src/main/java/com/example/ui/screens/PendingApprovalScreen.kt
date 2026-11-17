package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserSession
import com.example.ui.components.ClassRepCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.*
import com.example.util.JsonHelper
import com.example.util.RoleHelper

@Composable
fun PendingApprovalScreen(
  session: UserSession?,
  isLoading: Boolean,
  errorMessage: String?,
  infoMessage: String?,
  onDismissError: () -> Unit,
  onRefresh: () -> Unit,
  onLogout: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("pending_approval_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      Box(
        modifier = Modifier
          .size(84.dp)
          .clip(CircleShape)
          .background(GoldPrimary.copy(alpha = 0.14f))
          .border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.HourglassTop,
          contentDescription = "Awaiting Approval",
          tint = GoldPrimary,
          modifier = Modifier.size(42.dp)
        )
      }

      Spacer(modifier = Modifier.height(22.dp))

      Text(
        text = "Awaiting admin approval",
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("pending_approval_title")
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Your account has been created and is waiting for an administrator to approve your request. Once approved, access will update automatically.",
        fontSize = 14.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
      )

      Spacer(modifier = Modifier.height(20.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(14.dp))
      }

      if (!infoMessage.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GoldPrimary.copy(alpha = 0.15f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp)
        ) {
          Text(text = infoMessage, color = GoldLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      if (session != null) {
        ClassRepCard(modifier = Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PendingInfoRow("Name", JsonHelper.orDash(session.fullName))
            PendingInfoRow("Email", JsonHelper.orDash(session.email))
            PendingInfoRow("Role", RoleHelper.formatDisplayName(session.role))
            if (session.staffNumber.isNotBlank()) {
              PendingInfoRow("Staff Number", session.staffNumber)
            }
            if (session.department.isNotBlank()) {
              PendingInfoRow("Department", session.department)
            }
            PendingInfoRow("Status", "Pending Approval")
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }

      PrimaryButton(
        text = "Refresh Status",
        leadingIcon = Icons.Default.Refresh,
        onClick = onRefresh,
        isLoading = isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pending_approval_refresh_btn")
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedButton(
        onClick = onLogout,
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("pending_approval_logout_btn"),
        shape = RoundedCornerShape(ButtonRadius),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
        border = BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.8f))
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = ErrorRed
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Log Out", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun PendingInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, fontSize = 13.sp, color = TextSecondary)
    Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
  }
}
