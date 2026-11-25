package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.data.AdminUserItem
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdminVerifyLecturerConfirmDialog(
  user: AdminUserItem,
  targetVerified: Boolean,
  isActionLoading: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    title = {
      Text(
        text = if (targetVerified) "Restore Lecturer Access" else "Suspend Lecturer Access",
        color = GoldLight,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
      )
    },
    text = {
      Text(
        text = if (targetVerified) {
          "Restore lecturer access for ${user.fullName}?"
        } else {
          "Suspend lecturer access for ${user.fullName}? They will not be able to manage classes until restored."
        },
        color = TextSecondary
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = !isActionLoading,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (targetVerified) GoldPrimary else ErrorRed,
          contentColor = DarkTextOnGold
        )
      ) {
        Text(if (targetVerified) "Restore" else "Suspend")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) {
        Text("Cancel", color = TextSecondary)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}
