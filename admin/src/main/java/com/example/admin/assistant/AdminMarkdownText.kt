package com.example.admin.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.ui.theme.AdminBodyStyle
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.Midnight
import com.example.admin.ui.theme.SilverWhite

private val ADMIN_HTML_TAG_REGEX = Regex("<\\s*/?\\s*[a-zA-Z!][^>]*>")
private val ADMIN_SCRIPT_SCHEME_REGEX = Regex("(?i)(javascript:|data:|vbscript:)")

internal sealed interface AdminMarkdownBlock {
  data class Paragraph(val text: String) : AdminMarkdownBlock
  data class Bullet(val text: String) : AdminMarkdownBlock
  data class Numbered(val numberLabel: String, val text: String) : AdminMarkdownBlock
  data class CodeBlock(val language: String, val code: String) : AdminMarkdownBlock
}

internal object AdminSafeMarkdownParser {
  private val NUMBERED_REGEX = Regex("^(\\d+)\\.\\s+(.*)$")

  fun sanitizeText(raw: String): String {
    if (raw.isEmpty()) return ""
    return raw
      .replace(ADMIN_HTML_TAG_REGEX, "")
      .replace(ADMIN_SCRIPT_SCHEME_REGEX, "")
      .replace("<", "")
      .replace(">", "")
  }

  fun parseBlocks(markdown: String): List<AdminMarkdownBlock> {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<AdminMarkdownBlock>()
    var inCodeFence = false
    var codeLang = ""
    val codeLines = mutableListOf<String>()

    for (rawLine in lines) {
      val trimmed = rawLine.trim()
      if (trimmed.startsWith("```")) {
        if (!inCodeFence) {
          inCodeFence = true
          codeLang = trimmed.removePrefix("```").trim()
          codeLines.clear()
        } else {
          blocks.add(AdminMarkdownBlock.CodeBlock(codeLang, codeLines.joinToString("\n")))
          inCodeFence = false
          codeLang = ""
          codeLines.clear()
        }
        continue
      }
      if (inCodeFence) {
        codeLines.add(rawLine)
        continue
      }
      if (trimmed.isEmpty()) continue
      val clean = sanitizeText(trimmed)
      val numberedMatch = NUMBERED_REGEX.matchEntire(clean)
      when {
        clean.startsWith("- ") || clean.startsWith("* ") -> {
          blocks.add(AdminMarkdownBlock.Bullet(clean.drop(2).trim()))
        }
        clean.startsWith("• ") -> {
          blocks.add(AdminMarkdownBlock.Bullet(clean.drop(2).trim()))
        }
        numberedMatch != null -> {
          blocks.add(
            AdminMarkdownBlock.Numbered(
              numberLabel = "${numberedMatch.groupValues[1]}.",
              text = numberedMatch.groupValues[2].trim()
            )
          )
        }
        clean.startsWith("### ") || clean.startsWith("## ") || clean.startsWith("# ") -> {
          val headingText = clean.dropWhile { it == '#' }.trim()
          if (headingText.isNotEmpty()) blocks.add(AdminMarkdownBlock.Paragraph("**$headingText**"))
        }
        else -> blocks.add(AdminMarkdownBlock.Paragraph(clean))
      }
    }
    if (inCodeFence && codeLines.isNotEmpty()) {
      blocks.add(AdminMarkdownBlock.CodeBlock(codeLang, codeLines.joinToString("\n")))
    }
    return blocks
  }

  fun parseBoldInline(text: String, boldColor: Color): AnnotatedString {
    val safe = sanitizeText(text)
    return buildAnnotatedString {
      var idx = 0
      while (idx < safe.length) {
        val start = safe.indexOf("**", idx)
        if (start < 0) {
          append(safe.substring(idx))
          break
        }
        val end = safe.indexOf("**", start + 2)
        if (end < 0) {
          append(safe.substring(idx))
          break
        }
        if (start > idx) append(safe.substring(idx, start))
        val boldContent = safe.substring(start + 2, end)
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor))
        append(boldContent)
        pop()
        idx = end + 2
      }
    }
  }
}

@Composable
fun AdminMarkdownText(
  markdown: String,
  modifier: Modifier = Modifier,
  textColor: Color = SilverWhite,
  boldColor: Color = ChampagneGold
) {
  val blocks = remember(markdown) { AdminSafeMarkdownParser.parseBlocks(markdown) }
  val style = AdminBodyStyle.copy(fontSize = 15.sp, lineHeight = 22.sp, color = textColor)

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
    if (blocks.isEmpty()) {
      Text(text = AdminSafeMarkdownParser.sanitizeText(markdown), style = style)
    } else {
      blocks.forEach { block ->
        when (block) {
          is AdminMarkdownBlock.Paragraph -> {
            Text(text = AdminSafeMarkdownParser.parseBoldInline(block.text, boldColor), style = style)
          }
          is AdminMarkdownBlock.Bullet -> {
            Row(modifier = Modifier.padding(start = 4.dp)) {
              Text(text = "•", style = style.copy(color = boldColor, fontWeight = FontWeight.Bold))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = AdminSafeMarkdownParser.parseBoldInline(block.text, boldColor),
                style = style,
                modifier = Modifier.weight(1f)
              )
            }
          }
          is AdminMarkdownBlock.Numbered -> {
            Row(modifier = Modifier.padding(start = 4.dp)) {
              Text(
                text = block.numberLabel,
                style = style.copy(color = boldColor, fontWeight = FontWeight.SemiBold)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = AdminSafeMarkdownParser.parseBoldInline(block.text, boldColor),
                style = style,
                modifier = Modifier.weight(1f)
              )
            }
          }
          is AdminMarkdownBlock.CodeBlock -> {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Midnight.copy(alpha = 0.75f))
                .border(1.dp, AdminBorder, RoundedCornerShape(10.dp))
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (block.language.isNotBlank()) {
                Text(
                  text = block.language.uppercase(),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Lavender
                )
              }
              Text(
                text = block.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = textColor,
                modifier = Modifier.horizontalScroll(rememberScrollState())
              )
            }
          }
        }
      }
    }
  }
}
