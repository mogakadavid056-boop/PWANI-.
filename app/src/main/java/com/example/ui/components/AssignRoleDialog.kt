package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminUserItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AssignRoleDialog(
  user: AdminUserItem,
  selectedRoleChoice: String,
  showConfirmDialog: Boolean,
  isActionLoading: Boolean,
  onSelectRole: (String) -> Unit,
  onPromptConfirm: () -> Unit,
  onDismissConfirm: () -> Unit,
  onConfirmSave: () -> Unit,
  onDismiss: () -> Unit
) {
  val roleOptions = listOf(
    Pair("student", "Student"),
    Pair("lecturer", "Lecturer"),
    Pair("admin", "Super Admin")
  )

  if (showConfirmDialog) {
    val isSuperAdminSelected = selectedRoleChoice == "admin"
    val chosenDisplayRole = if (isSuperAdminSelected) "Super Admin" else roleOptions.find { it.first == selectedRoleChoice }?.second ?: "Student"

    AlertDialog(
      onDismissRequest = { if (!isActionLoading) onDismissConfirm() },
      containerColor = DarkCard,
      shape = RoundedCornerShape(16.dp),
      title = {
        Text(
          text = if (isSuperAdminSelected) "Confirm Super Admin Assignment" else "Confirm Role Change",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = if (isSuperAdminSelected) GoldPrimary else TextPrimary
        )
      },
      text = {
        Column {
          Text(
            text = "Are you sure you want to assign the role of $chosenDisplayRole to ${user.fullName.ifBlank { "this user" }}?",
            fontSize = 14.sp,
            color = TextPrimary,
            lineHeight = 20.sp
          )

          if (isSuperAdminSelected) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ErrorContainer)
                .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(12.dp)
            ) {
              Row(verticalAlignment = Alignment.Top) {
                Icon(
                  imageVector = Icons.Default.WarningAmber,
                  contentDescription = null,
                  tint = ErrorRed,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Warning: Super Admin privileges grant full administrative access across the entire system, including managing all users and classes.",
                  color = GoldLight,
                  fontSize = 12.sp,
                  lineHeight = 17.sp
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = onConfirmSave,
          enabled = !isActionLoading,
          colors = ButtonDefaults.buttonColors(
            containerColor = GoldPrimary,
            contentColor = com.example.ui.theme.DarkTextOnGold
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("admin_confirm_role_button")
        ) {
          if (isActionLoading) {
            CircularProgressIndicator(color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
          } else {
            Text("Confirm", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.DarkTextOnGold)
          }
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = onDismissConfirm,
          enabled = !isActionLoading,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = TextPrimary),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("admin_cancel_confirm_button")
        ) {
          Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
      }
    )
  } else {
    AlertDialog(
      onDismissRequest = onDismiss,
      containerColor = DarkCard,
      shape = RoundedCornerShape(16.dp),
      title = {
        Column {
          Text("Assign User Role", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoldLight)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = user.fullName.ifBlank { "User" },
            fontSize = 13.sp,
            color = TextSecondary
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          roleOptions.forEach { (roleValue, roleLabel) ->
            val isSelected = selectedRoleChoice == roleValue
            val isOptionSuperAdmin = roleValue == "admin"
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) DarkSurfaceVariant else DarkCard)
                .border(1.dp, if (isSelected) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
                .clickable { onSelectRole(roleValue) }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("role_option_$roleValue"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isSelected,
                onClick = { onSelectRole(roleValue) },
                colors = RadioButtonDefaults.colors(
                  selectedColor = GoldPrimary,
                  unselectedColor = TextSecondary
                )
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = roleLabel,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isOptionSuperAdmin && isSelected) GoldPrimary else TextPrimary
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = onPromptConfirm,
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("admin_save_role_button")
        ) {
          Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.DarkTextOnGold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = TextPrimary),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("admin_dismiss_role_dialog")
        ) {
          Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
      }
    )
  }
}
