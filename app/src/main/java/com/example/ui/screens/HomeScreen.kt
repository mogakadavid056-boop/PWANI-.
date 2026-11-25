package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassItem
import com.example.data.ClassWaitingForPrefect
import com.example.data.NoticeItem
import com.example.data.UserSession
import com.example.ui.components.ClassRepOutlinedButton
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.ClassesWaitingForPrefectCard
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.LatestAnnouncementsSection
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoleBadge
import com.example.ui.components.RoyalCard
import com.example.ui.components.RoyalDivider
import com.example.ui.theme.*
import com.example.util.RoleHelper
import java.util.Calendar

@Composable
fun HomeScreen(
  session: UserSession?,
  latestAnnouncements: List<NoticeItem> = emptyList(),
  isLatestLoading: Boolean = false,
  classes: List<ClassItem> = emptyList(),
  isClassesLoading: Boolean = false,
  classesError: String? = null,
  classesWaitingForRep: List<ClassWaitingForPrefect> = emptyList(),
  isWaitingLoading: Boolean = false,
  papersToMark: List<com.example.data.PaperToMarkItem> = emptyList(),
  isPapersToMarkLoading: Boolean = false,
  onOpenPaperToMark: (com.example.data.PaperToMarkItem) -> Unit = {},
  authInfoMessage: String? = null,
  onDismissAuthInfo: () -> Unit = {},
  unreadNotificationCount: Int = 0,
  onOpenNotifications: () -> Unit = {},
  isOnline: Boolean = true,
  onOpenAssistant: () -> Unit = {},
  onAppointPrefectClick: (ClassWaitingForPrefect) -> Unit = {},
  onDismissClassesError: () -> Unit = {},
  onRefreshClasses: () -> Unit = {},
  onNavigateToJoinClass: () -> Unit = {},
  onNavigateToCreateClass: () -> Unit = {},
  onNavigateToFindClasses: () -> Unit = {},
  onNavigateToMyClasses: () -> Unit = {},
  onNavigateToManageUsers: () -> Unit = {},
  onNavigateToProfile: () -> Unit = {},
  onSelectClass: (ClassItem) -> Unit = {},
  onNoticeClick: (NoticeItem) -> Unit = {},
  onLogout: () -> Unit = {}
) {
  val rawRole = session?.role.orEmpty()
  val isAdmin = RoleHelper.isAdmin(rawRole)
  val isLecturer = RoleHelper.isLecturer(rawRole)
  val isStaff = RoleHelper.isStaffRole(rawRole)
  val isVerified = session?.isVerified == true
  val displayRole = RoleHelper.formatDisplayName(rawRole)

  val firstName = session?.fullName?.trim()
    ?.split("\\s+".toRegex())
    ?.firstOrNull()
    ?.ifBlank { "Member" } ?: "Member"

  val timeGreeting = remember {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    when {
      hour < 12 -> "Good morning,"
      hour < 17 -> "Good afternoon,"
      else -> "Good evening,"
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("home_screen")
  ) {
    ClassRepPullToRefresh(
      isRefreshing = isLatestLoading || isClassesLoading,
      onRefresh = onRefreshClasses
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing)
      ) {
        // Hero Greeting & Role Card
        RoyalCard(isHero = true) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = timeGreeting,
                style = GreetingStyle.copy(fontSize = 30.sp, lineHeight = 36.sp)
              )
              Text(
                text = firstName,
                style = ProfileNameStyle,
                modifier = Modifier.testTag("home_greeting_text")
              )
              Spacer(modifier = Modifier.height(6.dp))
              RoleBadge(
                roleText = displayRole,
                modifier = Modifier.testTag("home_role_chip")
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              if (isOnline) {
                IconButton(
                  onClick = onOpenAssistant,
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RoyalSurfaceHigh)
                    .border(1.dp, MetallicGold.copy(alpha = 0.55f), CircleShape)
                    .testTag("home_assistant_button")
                ) {
                  Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Assistant",
                    tint = ChampagneGold,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
              NotificationBellButton(
                unreadCount = unreadNotificationCount,
                onClick = onOpenNotifications
              )
              IconButton(
                onClick = onNavigateToProfile,
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(RoyalSurfaceHigh)
                  .border(1.dp, DarkBorder, CircleShape)
                  .testTag("home_profile_avatar_button")
              ) {
                Icon(
                  Icons.Default.Person,
                  contentDescription = "Profile",
                  tint = ChampagneGold,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }
        }

        if (!classesError.isNullOrBlank()) {
          FriendlyErrorSnackbar(errorMessage = classesError, onDismiss = onDismissClassesError)
        }

        if (!authInfoMessage.isNullOrBlank()) {
          RoyalCard(
            borderColor = MetallicGold.copy(alpha = 0.65f),
            backgroundColor = RoyalSurfaceHigh,
            modifier = Modifier.testTag("home_auth_info_banner")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = authInfoMessage,
                style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, color = ChampagneGold),
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = onDismissAuthInfo) {
                Text("OK", style = FieldLabelStyle.copy(color = MetallicGold))
              }
            }
          }
        }

        // Lecturer suspension banner if verified == false
        if (isLecturer && !isVerified) {
          RoyalCard(
            borderColor = ErrorRed.copy(alpha = 0.8f),
            backgroundColor = RoyalSurfaceHigh,
            modifier = Modifier.testTag("home_lecturer_unverified_banner")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Your lecturer access is currently suspended. Contact an administrator.",
                style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = ChampagneGold)
              )
            }
          }
        }

        // Quick Action Buttons depending on user role and status
        if (isLecturer) {
          if (!isVerified) {
            ClassRepOutlinedButton(
              text = "My classes",
              leadingIcon = Icons.Default.Class,
              onClick = onNavigateToMyClasses,
              modifier = Modifier.fillMaxWidth().testTag("home_my_classes_button")
            )
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              PrimaryButton(
                text = "Join with code",
                leadingIcon = Icons.Default.GroupAdd,
                onClick = onNavigateToJoinClass,
                modifier = Modifier.weight(1f).testTag("home_join_class_button")
              )
              ClassRepOutlinedButton(
                text = "My classes",
                leadingIcon = Icons.Default.Class,
                onClick = onNavigateToMyClasses,
                modifier = Modifier.weight(1f).testTag("home_my_classes_button")
              )
            }
          }
        } else if (isAdmin) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            PrimaryButton(
              text = "Find classes",
              leadingIcon = Icons.Default.Search,
              onClick = onNavigateToFindClasses,
              modifier = Modifier.weight(1f).testTag("home_admin_find_classes_button")
            )
            ClassRepOutlinedButton(
              text = "Create Class",
              leadingIcon = Icons.Default.Add,
              onClick = onNavigateToCreateClass,
              modifier = Modifier.weight(1f).testTag("home_create_class_button")
            )
          }
        } else {
          val studentHasClass = classes.isNotEmpty()
          if (!studentHasClass) {
            PrimaryButton(
              text = "Join Class",
              leadingIcon = Icons.Default.GroupAdd,
              onClick = onNavigateToJoinClass,
              modifier = Modifier.fillMaxWidth().testTag("home_join_class_button")
            )
          }
        }

        if (isAdmin) {
          ClassRepOutlinedButton(
            text = "Manage Users",
            leadingIcon = Icons.Default.SupervisorAccount,
            onClick = onNavigateToManageUsers,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("home_manage_users_button")
          )
        }

        if (isOnline) {
          RoyalCard(
            borderColor = MetallicGold.copy(alpha = 0.6f),
            onClick = onOpenAssistant,
            modifier = Modifier.testTag("home_ask_assistant_card")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ChampagneGold,
                modifier = Modifier.size(26.dp)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text("Ask ClassRep Assistant", style = CardTitleStyle.copy(color = ChampagneGold))
                Text(
                  "Timetable, attendance, points, drafts and study help",
                  style = CaptionStyle.copy(color = Lavender)
                )
              }
            }
          }
        }

        RoyalDivider()

        if (isLecturer || isAdmin) {
          com.example.ui.components.PapersToMarkCard(
            papers = papersToMark,
            isLoading = isPapersToMarkLoading,
            onOpenExamMarking = onOpenPaperToMark
          )
        }

        if (isStaff) {
          ClassesWaitingForPrefectCard(
            classes = classesWaitingForRep,
            isLoading = isWaitingLoading,
            onAppointClick = onAppointPrefectClick
          )
        }

        LatestAnnouncementsSection(
          announcements = latestAnnouncements,
          isLoading = isLatestLoading,
          onNoticeClick = onNoticeClick
        )

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}
