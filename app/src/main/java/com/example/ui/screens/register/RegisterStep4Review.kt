package com.example.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GoldButton
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep4Review(
  state: RegisterWizardState,
  onToggleTerms: (Boolean) -> Unit
) {
  var showTermsSheet by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth()) {
    Text("Review", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GoldLight)
    Spacer(modifier = Modifier.height(6.dp))
    Text("Check your details before creating your account.", fontSize = 14.sp, color = TextSecondary)
    Spacer(modifier = Modifier.height(18.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(DarkCard)
        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        .padding(16.dp)
        .testTag("register_review_summary_card")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryRow("Role", state.role?.label ?: "Student")
        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
        SummaryRow("Full Name", state.fullName.trim())
        if (state.isLecturer) {
          if (state.idNumber.isNotBlank()) SummaryRow("Staff Number", state.idNumber.trim())
        } else {
          SummaryRow("Admission No.", state.idNumber.trim())
        }
        SummaryRow("Email", state.email.trim().lowercase())
        if (state.phone.isNotBlank()) SummaryRow("Phone", state.phone.trim())
        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
        if (state.isLecturer) {
          SummaryRow("School", state.selectedSchool?.name ?: "Not specified")
          SummaryRow("Passcode", "Entered")
        } else if (state.knowsDetails) {
          SummaryRow("School", state.selectedSchool?.name ?: "-")
          SummaryRow("Programme", state.selectedProgramme?.name ?: "-")
          SummaryRow("Year", state.selectedYearOfStudy?.let { "Year $it" } ?: "-")
        } else {
          SummaryRow("Class Code", state.classCode.trim().uppercase())
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkCard)
        .border(1.dp, if (state.agreedToTerms) GoldPrimary.copy(alpha = 0.7f) else DarkBorder, RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(
        checked = state.agreedToTerms,
        onCheckedChange = onToggleTerms,
        colors = CheckboxDefaults.colors(
          checkedColor = GoldPrimary,
          checkmarkColor = com.example.ui.theme.DarkTextOnGold,
          uncheckedColor = TextSecondary
        ),
        modifier = Modifier.testTag("register_terms_checkbox")
      )
      Spacer(modifier = Modifier.width(6.dp))
      val label = buildAnnotatedString {
        append("I agree to the ")
        withStyle(SpanStyle(color = GoldPrimary, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
          append("Terms and Privacy Policy")
        }
      }
      Text(
        text = label,
        fontSize = 13.sp,
        color = TextPrimary,
        modifier = Modifier
          .weight(1f)
          .clickable { showTermsSheet = true }
          .padding(vertical = 6.dp)
          .testTag("register_terms_link")
      )
    }
  }

  if (showTermsSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
      onDismissRequest = { showTermsSheet = false },
      sheetState = sheetState,
      containerColor = androidx.compose.ui.graphics.Color(0xFF0B2A1E)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 12.dp)
      ) {
        Text("Terms and Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "ClassRep collects your name, university email, admission or staff number, and optional phone number solely for academic coordination, class notices, and attendance verification within your institution.",
          fontSize = 14.sp,
          color = TextPrimary,
          lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Attendance records and class memberships are visible only to authorized class prefects, lecturers, and university administrators according to institutional policy. By creating an account (Terms v1.0), you agree to provide accurate academic details and use attendance tools honestly.",
          fontSize = 14.sp,
          color = TextSecondary,
          lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        GoldButton(
          text = "Close",
          onClick = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { showTermsSheet = false }
          }
        )
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun SummaryRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(0.38f))
    Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(0.62f))
  }
}
