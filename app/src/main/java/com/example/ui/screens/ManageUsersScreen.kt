package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import com.example.data.AdminUserItem
import com.example.ui.components.AdminTopBar
import com.example.ui.components.AdminUserRow
import com.example.ui.components.AdminVerifyLecturerConfirmDialog
import com.example.ui.components.AssignRoleDialog
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.RoyalCard
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSuccessContainer
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor

@Composable
fun ManageUsersScreen(
  users: List<AdminUserItem>,
  searchQuery: String,
  isLoading: Boolean,
  isActionLoading: Boolean,
  errorMessage: String?,
  successMessage: String?,
  selectedUser: AdminUserItem?,
  selectedRoleChoice: String,
  showConfirmDialog: Boolean,
  verificationPendingUser: AdminUserItem? = null,
  verificationTargetStatus: Boolean = false,
  showVerificationConfirmDialog: Boolean = false,
  onSearchQueryChanged: (String) -> Unit,
  onRefresh: () -> Unit,
  onUserClick: (AdminUserItem) -> Unit,
  onSelectRole: (String) -> Unit,
  onPromptConfirmRole: () -> Unit,
  onDismissConfirmRole: () -> Unit,
  onConfirmSaveRole: () -> Unit,
  onDismissRoleDialog: () -> Unit,
  onToggleVerification: (AdminUserItem) -> Unit = {},
  onDismissVerificationDialog: () -> Unit = {},
  onConfirmToggleVerification: () -> Unit = {},
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onBack: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("manage_users_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      AdminTopBar(onBack = onBack, onRefresh = onRefresh)

      Spacer(modifier = Modifier.height(12.dp))

      val searchInteraction = remember { MutableInteractionSource() }
      val isSearchFocused by searchInteraction.collectIsFocusedAsState()
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        interactionSource = searchInteraction,
        modifier = Modifier
          .fillMaxWidth()
          .then(if (isSearchFocused) Modifier.shadow(10.dp, RoundedCornerShape(ButtonRadius), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
          .testTag("admin_search_input"),
        placeholder = {
          Text("Search by name, email, or admission no...", style = CaptionStyle.copy(color = Lavender.copy(alpha = 0.7f)))
        },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = if (isSearchFocused) ChampagneGold else MetallicGold, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChanged("") }, modifier = Modifier.size(48.dp)) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Lavender, modifier = Modifier.size(18.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(ButtonRadius),
        colors = royalOutlinedTextFieldColors()
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(10.dp))
      }

      if (!successMessage.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ButtonRadius))
            .background(RoyalSuccessContainer)
            .border(1.dp, SuccessColor.copy(alpha = 0.55f), RoundedCornerShape(ButtonRadius))
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = successMessage, style = CaptionStyle.copy(color = SuccessColor), modifier = Modifier.weight(1f))
            IconButton(onClick = onDismissSuccess, modifier = Modifier.size(48.dp)) {
              Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = SuccessColor, modifier = Modifier.size(16.dp))
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (searchQuery.isBlank()) "Users" else "Search Results",
          style = SectionHeadingStyle.copy(fontSize = 18.sp, color = SilverWhite)
        )
        Text(text = "${users.size} shown", style = CaptionStyle.copy(color = Lavender))
      }

      Spacer(modifier = Modifier.height(8.dp))

      ClassRepPullToRefresh(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
      ) {
        if (isLoading && users.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadingSkeleton(height = 65)
            LoadingSkeleton(height = 65)
            LoadingSkeleton(height = 65)
          }
        } else if (users.isEmpty()) {
          RoyalCard {
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Default.People, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(44.dp))
              Spacer(modifier = Modifier.height(10.dp))
              Text("No users found", style = SectionHeadingStyle.copy(fontSize = 18.sp))
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (searchQuery.isNotBlank()) "No users match '$searchQuery'" else "Pull down to refresh users",
                style = CaptionStyle.copy(color = Lavender)
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
          ) {
            items(users, key = { it.id }) { user ->
              AdminUserRow(
                user = user,
                onClick = { onUserClick(user) },
                onToggleVerification = { onToggleVerification(user) }
              )
            }
          }
        }
      }
    }

    if (selectedUser != null) {
      AssignRoleDialog(
        user = selectedUser,
        selectedRoleChoice = selectedRoleChoice,
        showConfirmDialog = showConfirmDialog,
        isActionLoading = isActionLoading,
        onSelectRole = onSelectRole,
        onPromptConfirm = onPromptConfirmRole,
        onDismissConfirm = onDismissConfirmRole,
        onConfirmSave = onConfirmSaveRole,
        onDismiss = onDismissRoleDialog
      )
    }

    if (showVerificationConfirmDialog && verificationPendingUser != null) {
      AdminVerifyLecturerConfirmDialog(
        user = verificationPendingUser,
        targetVerified = verificationTargetStatus,
        isActionLoading = isActionLoading,
        onConfirm = onConfirmToggleVerification,
        onDismiss = onDismissVerificationDialog
      )
    }
  }
}
