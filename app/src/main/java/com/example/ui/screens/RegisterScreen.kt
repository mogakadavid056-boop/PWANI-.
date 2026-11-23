package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GoldButton
import com.example.ui.components.MetallicGoldText
import com.example.ui.screens.register.RegisterStep1Role
import com.example.ui.screens.register.RegisterStep2Details
import com.example.ui.screens.register.RegisterStep3ClassOrLecturer
import com.example.ui.screens.register.RegisterStep4Review
import com.example.ui.screens.register.RegisterWizardState
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.ScreenTitleStyle

@Composable
fun RegisterScreen(
  isLoading: Boolean,
  errorMessage: String?,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  onSchoolSelected: (SchoolItem) -> Unit,
  onDismissError: () -> Unit,
  onSubmitWizard: (RegisterWizardState) -> Unit,
  onNavigateToLogin: () -> Unit,
  onBack: () -> Unit
) {
  var wizard by remember { mutableStateOf(RegisterWizardState()) }
  val animatedProgress by animateFloatAsState(
    targetValue = wizard.step / 4f,
    animationSpec = tween(260),
    label = "register_progress"
  )

  val goBackStep: () -> Unit = {
    if (!isLoading) {
      if (wizard.step > 1) {
        onDismissError()
        wizard = wizard.copy(step = wizard.step - 1, slideDirection = -1)
      } else {
        onBack()
      }
    }
  }

  BackHandler(enabled = !isLoading, onBack = goBackStep)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .imePadding()
      .testTag("register_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = goBackStep,
            enabled = !isLoading,
            modifier = Modifier.size(48.dp).testTag("register_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
          }
          Spacer(modifier = Modifier.width(4.dp))
          MetallicGoldText("Create Account", style = ScreenTitleStyle.copy(fontSize = 22.sp, lineHeight = 28.sp))
        }
        Text(
          text = "Step ${wizard.step} of 4",
          style = FieldLabelStyle.copy(color = MetallicGold),
          modifier = Modifier.testTag("register_step_label")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      LinearProgressIndicator(
        progress = { animatedProgress },
        color = MetallicGold,
        trackColor = RoyalSurface,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .testTag("register_progress_bar")
      )

      Spacer(modifier = Modifier.height(14.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(12.dp))
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        AnimatedContent(
          targetState = wizard.step,
          transitionSpec = {
            val dir = if (targetState >= initialState) 1 else -1
            (fadeIn(tween(240)) + slideInHorizontally(tween(240)) { dir * it / 2 }) togetherWith
              (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -dir * it / 2 })
          },
          label = "register_step_transition"
        ) { currentStep ->
          when (currentStep) {
            1 -> RegisterStep1Role(
              selectedRole = wizard.role,
              onSelectRole = { r -> wizard = wizard.copy(role = r) }
            )
            2 -> RegisterStep2Details(
              state = wizard,
              onUpdate = { transform -> wizard = transform(wizard) }
            )
            3 -> RegisterStep3ClassOrLecturer(
              state = wizard,
              schools = schools,
              programmes = programmes,
              onSchoolSelected = onSchoolSelected,
              onUpdate = { transform -> wizard = transform(wizard) }
            )
            else -> RegisterStep4Review(
              state = wizard,
              onToggleTerms = { checked -> wizard = wizard.copy(agreedToTerms = checked) }
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      Spacer(modifier = Modifier.height(10.dp))

      GoldButton(
        text = if (wizard.step < 4) "Next" else "Create Account",
        onClick = {
          if (isLoading) return@GoldButton
          onDismissError()
          if (wizard.step < 4) {
            wizard = wizard.copy(step = wizard.step + 1, slideDirection = 1)
          } else {
            onSubmitWizard(wizard)
          }
        },
        enabled = wizard.isCurrentStepValid && !isLoading,
        isLoading = isLoading,
        testTag = if (wizard.step < 4) "register_next_button" else "register_submit_button"
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Already have an account? ", style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, color = Lavender))
        Text(
          text = "Sign In",
          style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = MetallicGold),
          modifier = Modifier
            .clickable(enabled = !isLoading) { onNavigateToLogin() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("register_to_login_link")
        )
      }
    }
  }
}
