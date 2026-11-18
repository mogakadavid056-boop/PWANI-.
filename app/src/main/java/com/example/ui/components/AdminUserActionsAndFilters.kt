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
        text = if (targetVerified) "Verify Lecturer" else "Remove Verification",
        color = GoldLight,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
      )
    },
    text = {
      Text(
        text = if (targetVerified) {
          "Are you sure you want to verify ${user.fullName} as an official lecturer?"
        } else {
          "Are you sure you want to remove lecturer verification for ${user.fullName}?"
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
        Text(if (targetVerified) "Verify" else "Remove")
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
