package com.example.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderHighlight
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.MontserratFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor
import kotlinx.coroutines.delay

@Composable
fun AssistantMarkdownText(
  markdown: String,
  modifier: Modifier = Modifier,
  messageKey: String = "msg",
  textColor: Color = SilverWhite,
  boldColor: Color = ChampagneGold
) {
  val blocks = remember(markdown) { SafeMarkdownParser.parseBlocks(markdown) }
  val bodyStyle = BodyStyle.copy(fontSize = 15.5.sp, lineHeight = 23.5.sp, color = textColor)
  val codeInlineBg = Midnight.copy(alpha = 0.72f)

  SelectionContainer(modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (blocks.isEmpty()) {
        Text(text = SafeMarkdownParser.sanitizeText(markdown), style = bodyStyle)
      } else {
        blocks.forEachIndexed { index, block ->
          when (block) {
            is MarkdownBlock.Heading -> {
              HeadingBlockView(
                block = block,
                isFirstBlock = index == 0,
                boldColor = boldColor,
                textColor = textColor,
                codeInlineBg = codeInlineBg,
                testTag = "gemini_copy_section_${messageKey}_$index"
              )
            }
            is MarkdownBlock.Paragraph -> {
              Text(
                text = rememberInline(block.text, boldColor, textColor, codeInlineBg),
                style = bodyStyle
              )
            }
            is MarkdownBlock.Bullet -> {
              val indentDp = (block.indentLevel * 16).dp
              val bulletChar = when (block.indentLevel) {
                0 -> "•"
                1 -> "◦"
                else -> "▪"
              }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 4.dp + indentDp),
                verticalAlignment = Alignment.Top
              ) {
                Text(
                  text = bulletChar,
                  style = bodyStyle.copy(color = boldColor, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = rememberInline(block.text, boldColor, textColor, codeInlineBg),
                  style = bodyStyle,
                  modifier = Modifier.weight(1f)
                )
              }
            }
            is MarkdownBlock.Numbered -> {
              val indentDp = (block.indentLevel * 16).dp
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 4.dp + indentDp),
                verticalAlignment = Alignment.Top
              ) {
                Box(
                  modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(RoyalSurfaceHigh)
                    .border(0.75.dp, DarkBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                  Text(
                    text = block.numberLabel.removeSuffix("."),
                    style = FieldLabelStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, color = boldColor)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = rememberInline(block.text, boldColor, textColor, codeInlineBg),
                  style = bodyStyle,
                  modifier = Modifier.weight(1f)
                )
              }
            }
            is MarkdownBlock.StepBlock -> {
              StepBlockCard(
                block = block,
                boldColor = boldColor,
                textColor = textColor,
                codeInlineBg = codeInlineBg
              )
            }
            is MarkdownBlock.CodeBlock -> {
              CodeBlockCard(
                block = block,
                textColor = textColor,
                testTag = "gemini_copy_code_${messageKey}_$index"
              )
            }
            is MarkdownBlock.Table -> {
              TableBlockCard(
                block = block,
                boldColor = boldColor,
                textColor = textColor,
                codeInlineBg = codeInlineBg,
                testTag = "gemini_copy_table_${messageKey}_$index"
              )
            }
            is MarkdownBlock.EquationBlock -> {
              EquationBlockCard(
                block = block,
                textColor = textColor,
                testTag = "gemini_copy_equation_${messageKey}_$index"
              )
            }
            is MarkdownBlock.Blockquote -> {
              BlockquoteCard(
                block = block,
                boldColor = boldColor,
                textColor = textColor,
                codeInlineBg = codeInlineBg,
                testTag = "gemini_copy_quote_${messageKey}_$index"
              )
            }
            MarkdownBlock.Divider -> {
              HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 1.dp,
                color = DarkBorder
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun rememberInline(
  text: String,
  boldColor: Color,
  textColor: Color,
  codeInlineBg: Color
): AnnotatedString = remember(text, boldColor, textColor) {
  SafeMarkdownParser.parseInlineMarkdown(
    text = text,
    boldColor = boldColor,
    codeColor = ChampagneGold,
    codeBgColor = codeInlineBg,
    linkColor = ChampagneGold,
    mathColor = textColor
  )
}

@Composable
private fun HeadingBlockView(
  block: MarkdownBlock.Heading,
  isFirstBlock: Boolean,
  boldColor: Color,
  textColor: Color,
  codeInlineBg: Color,
  testTag: String
) {
  val topPadding = if (isFirstBlock) 2.dp else when (block.level) {
    1 -> 10.dp
    2 -> 8.dp
    else -> 4.dp
  }
  val headingStyle: TextStyle = when (block.level) {
    1 -> TextStyle(
      fontFamily = PlayfairDisplayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      lineHeight = 27.sp,
      color = boldColor
    )
    2 -> TextStyle(
      fontFamily = PlayfairDisplayFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 18.sp,
      lineHeight = 25.sp,
      color = boldColor
    )
    3 -> TextStyle(
      fontFamily = MontserratFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 15.5.sp,
      lineHeight = 22.sp,
      color = Lavender
    )
    else -> TextStyle(
      fontFamily = MontserratFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.5.sp,
      lineHeight = 20.sp,
      color = textColor
    )
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = topPadding),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = rememberInline(block.text, boldColor, textColor, codeInlineBg),
        style = headingStyle,
        modifier = Modifier.weight(1f)
      )
      if (!block.sectionCopyText.isNullOrBlank()) {
        DisableSelection {
          InlineCopyChip(
            textToCopy = block.sectionCopyText,
            label = "Section",
            contentDesc = "Copy section ${block.text}",
            testTag = testTag
          )
        }
      }
    }
    if (block.level == 1) {
      HorizontalDivider(thickness = 0.75.dp, color = DarkBorder)
    }
  }
}

@Composable
private fun StepBlockCard(
  block: MarkdownBlock.StepBlock,
  boldColor: Color,
  textColor: Color,
  codeInlineBg: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 4.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(RoyalSurfaceHigh.copy(alpha = 0.72f))
      .border(1.dp, DarkBorderHighlight.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(RoyalPurple)
        .border(1.dp, MetallicGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Text(
        text = "Step ${block.stepNumber}",
        style = FieldLabelStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, color = boldColor)
      )
    }
    if (block.title.isNotBlank()) {
      Text(
        text = rememberInline(block.title, boldColor, textColor, codeInlineBg),
        style = BodyStyle.copy(
          fontSize = 15.5.sp,
          lineHeight = 22.sp,
          fontWeight = FontWeight.SemiBold,
          color = textColor
        ),
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun CodeBlockCard(
  block: MarkdownBlock.CodeBlock,
  textColor: Color,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Midnight.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      DisableSelection {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(RoyalSurfaceHigh.copy(alpha = 0.85f))
            .padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = block.language.ifBlank { "code" }.uppercase(),
            style = FieldLabelStyle.copy(fontSize = 11.5.sp, color = Lavender)
          )
          InlineCopyChip(
            textToCopy = block.code,
            label = "Copy",
            contentDesc = "Copy code block",
            testTag = testTag
          )
        }
      }
      HorizontalDivider(thickness = 0.75.dp, color = DarkBorder)
      Text(
        text = block.code,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = textColor,
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(12.dp)
      )
    }
  }
}

@Composable
private fun TableBlockCard(
  block: MarkdownBlock.Table,
  boldColor: Color,
  textColor: Color,
  codeInlineBg: Color,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Midnight.copy(alpha = 0.78f),
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      DisableSelection {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(RoyalSurfaceHigh.copy(alpha = 0.85f))
            .padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "TABLE • ${block.rows.size} ROWS",
            style = FieldLabelStyle.copy(fontSize = 11.5.sp, color = Lavender)
          )
          InlineCopyChip(
            textToCopy = block.rawMarkdown,
            label = "Copy",
            contentDesc = "Copy table",
            testTag = testTag
          )
        }
      }
      HorizontalDivider(thickness = 0.75.dp, color = DarkBorder)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
      ) {
        // Header row
        Row(
          modifier = Modifier
            .background(RoyalSurfaceHigh.copy(alpha = 0.65f))
            .height(IntrinsicSize.Min)
        ) {
          block.headers.forEachIndexed { colIdx, header ->
            val align = toTextAlign(block.alignments.getOrElse(colIdx) { TableAlignment.LEFT })
            Text(
              text = rememberInline(header, boldColor, boldColor, codeInlineBg),
              style = FieldLabelStyle.copy(fontSize = 13.sp, lineHeight = 18.sp, color = boldColor),
              textAlign = align,
              modifier = Modifier
                .widthIn(min = 110.dp, max = 240.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            if (colIdx < block.headers.lastIndex) {
              Box(
                modifier = Modifier
                  .fillMaxHeight()
                  .width(0.75.dp)
                  .background(DarkBorder)
              )
            }
          }
        }

        // Data rows
        block.rows.forEachIndexed { rowIdx, row ->
          HorizontalDivider(thickness = 0.75.dp, color = DarkBorder.copy(alpha = 0.65f))
          val rowBg = if (rowIdx % 2 == 1) RoyalSurfaceHigh.copy(alpha = 0.28f) else Color.Transparent
          Row(
            modifier = Modifier
              .background(rowBg)
              .height(IntrinsicSize.Min)
          ) {
            row.forEachIndexed { colIdx, cell ->
              val align = toTextAlign(block.alignments.getOrElse(colIdx) { TableAlignment.LEFT })
              Text(
                text = rememberInline(cell, boldColor, textColor, codeInlineBg),
                style = BodyStyle.copy(fontSize = 13.5.sp, lineHeight = 19.sp, color = textColor),
                textAlign = align,
                modifier = Modifier
                  .widthIn(min = 110.dp, max = 240.dp)
                  .padding(horizontal = 10.dp, vertical = 8.dp)
              )
              if (colIdx < row.lastIndex) {
                Box(
                  modifier = Modifier
                    .fillMaxHeight()
                    .width(0.75.dp)
                    .background(DarkBorder.copy(alpha = 0.55f))
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EquationBlockCard(
  block: MarkdownBlock.EquationBlock,
  textColor: Color,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Midnight.copy(alpha = 0.78f),
    border = BorderStroke(1.dp, DarkBorderHighlight.copy(alpha = 0.55f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      DisableSelection {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(RoyalSurfaceHigh.copy(alpha = 0.8f))
            .padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "EQUATION",
            style = FieldLabelStyle.copy(fontSize = 11.5.sp, color = ChampagneGold)
          )
          InlineCopyChip(
            textToCopy = block.rawLatex,
            label = "Copy",
            contentDesc = "Copy equation",
            testTag = testTag
          )
        }
      }
      HorizontalDivider(thickness = 0.75.dp, color = DarkBorder)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        Text(
          text = block.formattedMath,
          fontFamily = FontFamily.Serif,
          fontStyle = FontStyle.Italic,
          fontSize = 16.5.sp,
          lineHeight = 24.sp,
          color = textColor
        )
      }
    }
  }
}

@Composable
private fun BlockquoteCard(
  block: MarkdownBlock.Blockquote,
  boldColor: Color,
  textColor: Color,
  codeInlineBg: Color,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = RoyalSurfaceHigh.copy(alpha = 0.62f),
    border = BorderStroke(0.75.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
    ) {
      Box(
        modifier = Modifier
          .width(4.dp)
          .fillMaxHeight()
          .background(MetallicGold)
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        DisableSelection {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "KEY NOTE",
              style = FieldLabelStyle.copy(fontSize = 11.sp, color = boldColor)
            )
            InlineCopyChip(
              textToCopy = block.rawText,
              label = "Copy",
              contentDesc = "Copy quote",
              testTag = testTag
            )
          }
        }
        block.lines.forEach { line ->
          Text(
            text = rememberInline(line, boldColor, textColor.copy(alpha = 0.94f), codeInlineBg),
            style = BodyStyle.copy(fontSize = 14.5.sp, lineHeight = 21.sp, color = textColor.copy(alpha = 0.94f))
          )
        }
      }
    }
  }
}

@Composable
private fun InlineCopyChip(
  textToCopy: String,
  label: String,
  contentDesc: String,
  testTag: String
) {
  val clipboard = LocalClipboardManager.current
  var copied by remember(textToCopy) { mutableStateOf(false) }

  LaunchedEffect(copied) {
    if (copied) {
      delay(1600)
      copied = false
    }
  }

  Box(
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(8.dp))
      .pointerHoverIcon(PointerIcon.Hand)
      .clickable {
        clipboard.setText(AnnotatedString(textToCopy))
        copied = true
      }
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
        contentDescription = if (copied) "Copied" else contentDesc,
        tint = if (copied) SuccessColor else Lavender,
        modifier = Modifier.size(14.dp)
      )
      Text(
        text = if (copied) "Copied" else label,
        style = FieldLabelStyle.copy(
          fontSize = 11.5.sp,
          lineHeight = 14.sp,
          color = if (copied) SuccessColor else Lavender
        )
      )
    }
  }
}

private fun toTextAlign(alignment: TableAlignment): TextAlign = when (alignment) {
  TableAlignment.LEFT -> TextAlign.Start
  TableAlignment.CENTER -> TextAlign.Center
  TableAlignment.RIGHT -> TextAlign.End
}
