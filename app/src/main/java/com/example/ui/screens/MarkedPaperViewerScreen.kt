package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCard
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.ScreenTitleStyle
import com.example.util.PdfPrintHelper
import java.io.File

@Composable
fun MarkedPaperViewerScreen(
  pdfFile: File,
  title: String,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  var pages by remember(pdfFile.absolutePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
  var renderError by remember(pdfFile.absolutePath) { mutableStateOf<String?>(null) }

  DisposableEffect(pdfFile.absolutePath) {
    val rendered = mutableListOf<Bitmap>()
    var descriptor: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    try {
      descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
      renderer = PdfRenderer(descriptor)
      for (i in 0 until renderer.pageCount) {
        renderer.openPage(i).use { page ->
          val scale = 2
          val w = (page.width * scale).coerceAtLeast(595)
          val h = (page.height * scale).coerceAtLeast(842)
          val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
          Canvas(bmp).drawColor(AndroidColor.WHITE)
          page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          rendered.add(bmp)
        }
      }
      pages = rendered
      renderError = null
    } catch (e: Exception) {
      renderError = e.localizedMessage ?: "Could not render PDF pages."
    } finally {
      runCatching { renderer?.close() }
      runCatching { descriptor?.close() }
    }

    onDispose {
      rendered.forEach { if (!it.isRecycled) it.recycle() }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag("marked_paper_viewer_screen"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("pdf_viewer_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        MetallicGoldText("Marked Paper", style = ScreenTitleStyle)
      }
    }

    RoyalCard(isHero = true) {
      Text(title.ifBlank { "Marked Exam Paper" }, style = CardTitleStyle)
      Text("${pages.size} page(s)", style = CaptionStyle)
      Spacer(modifier = Modifier.height(4.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { PdfPrintHelper.printPdfFile(context, pdfFile, title) },
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("pdf_print_btn")
        ) {
          Icon(Icons.Default.Print, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Print", style = ButtonTextStyle.copy(color = Midnight))
        }
        OutlinedButton(
          onClick = { PdfPrintHelper.sharePdfFile(context, pdfFile, title) },
          shape = RoundedCornerShape(ButtonRadius),
          border = BorderStroke(1.dp, ChampagneGold),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("pdf_share_btn")
        ) {
          Icon(Icons.Default.Share, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share PDF", style = ButtonTextStyle.copy(color = ChampagneGold))
        }
      }
    }

    if (!renderError.isNullOrBlank()) {
      FriendlyErrorSnackbar(errorMessage = renderError!!, onDismiss = { renderError = null })
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (pages.isEmpty() && renderError == null) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MetallicGold)
        }
      } else {
        pages.forEachIndexed { idx, bmp ->
          val ratio = if (bmp.height > 0) bmp.width.toFloat() / bmp.height.toFloat() else (595f / 842f)
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Marked paper page ${idx + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(ratio)
              .clip(RoundedCornerShape(12.dp))
              .background(androidx.compose.ui.graphics.Color.White)
              .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
