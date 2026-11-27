package com.example.assistant

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

enum class TableAlignment {
  LEFT,
  CENTER,
  RIGHT
}

internal sealed interface MarkdownBlock {
  data class Heading(
    val level: Int,
    val text: String,
    val sectionCopyText: String? = null
  ) : MarkdownBlock

  data class Paragraph(val text: String) : MarkdownBlock

  data class Bullet(
    val text: String,
    val indentLevel: Int = 0
  ) : MarkdownBlock

  data class Numbered(
    val numberLabel: String,
    val text: String,
    val indentLevel: Int = 0
  ) : MarkdownBlock

  data class StepBlock(
    val stepNumber: String,
    val title: String,
    val body: String
  ) : MarkdownBlock

  data class CodeBlock(
    val language: String,
    val code: String
  ) : MarkdownBlock

  data class Table(
    val headers: List<String>,
    val alignments: List<TableAlignment>,
    val rows: List<List<String>>,
    val rawMarkdown: String
  ) : MarkdownBlock

  data class EquationBlock(
    val rawLatex: String,
    val formattedMath: String
  ) : MarkdownBlock

  data class Blockquote(
    val lines: List<String>,
    val rawText: String
  ) : MarkdownBlock

  data object Divider : MarkdownBlock
}

internal object LatexMathFormatter {
  private val SUPERSCRIPTS = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
    '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
    'n' to 'ⁿ', 'i' to 'ⁱ'
  )

  private val SUBSCRIPTS = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
    '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
    '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
    'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
    'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
    'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
    'v' to 'ᵥ', 'x' to 'ₓ'
  )

  private val SYMBOL_MAP = listOf(
    "\\Longleftrightarrow" to "⟺",
    "\\Leftrightarrow" to "⇔",
    "\\Longrightarrow" to "⟹",
    "\\longrightarrow" to "⟶",
    "\\longleftarrow" to "⟵",
    "\\leftrightarrow" to "↔",
    "\\Rightarrow" to "⇒",
    "\\Leftarrow" to "⇐",
    "\\rightarrow" to "→",
    "\\leftarrow" to "←",
    "\\implies" to "⇒",
    "\\mapsto" to "↦",
    "\\iff" to "⇔",
    "\\to" to "→",
    "\\varepsilon" to "ε",
    "\\vartheta" to "ϑ",
    "\\varphi" to "φ",
    "\\varrho" to "ϱ",
    "\\varsigma" to "ς",
    "\\Upsilon" to "Υ",
    "\\Lambda" to "Λ",
    "\\Sigma" to "Σ",
    "\\Gamma" to "Γ",
    "\\Delta" to "Δ",
    "\\Theta" to "Θ",
    "\\Omega" to "Ω",
    "\\alpha" to "α",
    "\\beta" to "β",
    "\\gamma" to "γ",
    "\\delta" to "δ",
    "\\epsilon" to "ε",
    "\\zeta" to "ζ",
    "\\eta" to "η",
    "\\theta" to "θ",
    "\\iota" to "ι",
    "\\kappa" to "κ",
    "\\lambda" to "λ",
    "\\mu" to "μ",
    "\\nu" to "ν",
    "\\xi" to "ξ",
    "\\pi" to "π",
    "\\rho" to "ρ",
    "\\sigma" to "σ",
    "\\tau" to "τ",
    "\\upsilon" to "υ",
    "\\phi" to "φ",
    "\\chi" to "χ",
    "\\psi" to "ψ",
    "\\omega" to "ω",
    "\\Phi" to "Φ",
    "\\Psi" to "Ψ",
    "\\Xi" to "Ξ",
    "\\Pi" to "Π",
    "\\times" to "×",
    "\\cdot" to "·",
    "\\div" to "÷",
    "\\pm" to "±",
    "\\mp" to "∓",
    "\\leq" to "≤",
    "\\geq" to "≥",
    "\\neq" to "≠",
    "\\approx" to "≈",
    "\\equiv" to "≡",
    "\\cong" to "≅",
    "\\sim" to "~",
    "\\propto" to "∝",
    "\\le" to "≤",
    "\\ge" to "≥",
    "\\ne" to "≠",
    "\\ll" to "≪",
    "\\gg" to "≫",
    "\\infty" to "∞",
    "\\partial" to "∂",
    "\\nabla" to "∇",
    "\\forall" to "∀",
    "\\exists" to "∃",
    "\\nexists" to "∄",
    "\\notin" to "∉",
    "\\subseteq" to "⊆",
    "\\supseteq" to "⊇",
    "\\subset" to "⊂",
    "\\supset" to "⊃",
    "\\setminus" to "∖",
    "\\emptyset" to "∅",
    "\\varnothing" to "∅",
    "\\in" to "∈",
    "\\cup" to "∪",
    "\\cap" to "∩",
    "\\land" to "∧",
    "\\lor" to "∨",
    "\\neg" to "¬",
    "\\therefore" to "∴",
    "\\because" to "∵",
    "\\degree" to "°",
    "\\circ" to "°",
    "\\angle" to "∠",
    "\\perp" to "⊥",
    "\\parallel" to "∥",
    "\\sum" to "∑",
    "\\prod" to "∏",
    "\\oint" to "∮",
    "\\iint" to "∬",
    "\\int" to "∫",
    "\\ldots" to "…",
    "\\cdots" to "⋯",
    "\\dots" to "…",
    "\\left(" to "(",
    "\\right)" to ")",
    "\\left[" to "[",
    "\\right]" to "]",
    "\\left\\{" to "{",
    "\\right\\}" to "}",
    "\\left|" to "|",
    "\\right|" to "|",
    "\\left" to "",
    "\\right" to "",
    "\\quad" to "  ",
    "\\qquad" to "    ",
    "\\," to " ",
    "\\;" to " ",
    "\\!" to "",
    "\\%" to "%",
    "\\&" to "&",
    "\\$" to "$"
  )

  private val TEXT_WRAPPER_REGEX = Regex("\\\\(?:text|mathrm|mathbf|mathit|mathsf|textbf|textit)\\{([^{}]*)\\}")
  private val SQRT_N_REGEX = Regex("\\\\sqrt\\[([^\\]]+)\\]\\{([^{}]*)\\}")
  private val SQRT_REGEX = Regex("\\\\sqrt\\{([^{}]*)\\}")

  fun format(rawLatex: String): String {
    var out = rawLatex.trim()
    if (out.isEmpty()) return ""

    // Iteratively unwrap \frac{a}{b} (supports nested braces cleanly)
    out = replaceFractions(out)

    // Unwrap \sqrt[n]{x} and \sqrt{x}
    repeat(3) {
      out = out.replace(SQRT_N_REGEX) { m ->
        val n = toSuperscript(m.groupValues[1].trim())
        val inner = m.groupValues[2].trim()
        "${n}√($inner)"
      }
      out = out.replace(SQRT_REGEX) { m ->
        val inner = m.groupValues[1].trim()
        if (inner.length <= 2 && inner.all { it.isLetterOrDigit() }) "√$inner" else "√($inner)"
      }
    }

    // Unwrap \text{...}, \mathrm{...}, etc.
    repeat(3) {
      out = out.replace(TEXT_WRAPPER_REGEX) { it.groupValues[1] }
    }

    // Standard math function names
    out = out
      .replace("\\sin", "sin")
      .replace("\\cos", "cos")
      .replace("\\tan", "tan")
      .replace("\\cot", "cot")
      .replace("\\sec", "sec")
      .replace("\\csc", "csc")
      .replace("\\log", "log")
      .replace("\\ln", "ln")
      .replace("\\exp", "exp")
      .replace("\\lim", "lim")
      .replace("\\max", "max")
      .replace("\\min", "min")
      .replace("\\det", "det")
      .replace("\\gcd", "gcd")
      .replace("\\mod", " mod ")
      .replace("\\bar{x}", "x̄")
      .replace("\\hat{y}", "ŷ")

    // Replace symbolic LaTeX tokens (longest tokens first)
    for ((token, replacement) in SYMBOL_MAP) {
      if (out.contains(token)) {
        out = out.replace(token, replacement)
      }
    }

    // Convert ^{...}, ^x, _{...}, _x
    out = replaceScripts(out, isSuper = true)
    out = replaceScripts(out, isSuper = false)

    // Clean up remaining trivial braces {x} -> x
    out = out.replace(Regex("\\{([^{}]{1,24})\\}")) { it.groupValues[1] }
    return out.replace(Regex("[ \\t]+"), " ").trim()
  }

  private fun replaceFractions(input: String): String {
    var current = input
    var guard = 0
    while (guard++ < 12) {
      val idx = current.indexOf("\\frac")
      val dIdx = if (idx < 0) current.indexOf("\\dfrac") else -1
      val pos = if (idx >= 0) idx else dIdx
      if (pos < 0) break
      val cmdLen = if (idx >= 0) 5 else 6
      val afterCmd = skipSpaces(current, pos + cmdLen)
      val numPair = readBraced(current, afterCmd) ?: break
      val afterNum = skipSpaces(current, numPair.second)
      val denPair = readBraced(current, afterNum) ?: break

      val num = numPair.first.trim()
      val den = denPair.first.trim()
      val numFmt = if (needsParens(num)) "($num)" else num
      val denFmt = if (needsParens(den)) "($den)" else den
      current = current.substring(0, pos) + "$numFmt/$denFmt" + current.substring(denPair.second)
    }
    return current
  }

  private fun needsParens(expr: String): Boolean {
    if (expr.length <= 1) return false
    return expr.any { it == '+' || it == '-' || it == ' ' || it == '*' || it == '/' }
  }

  private fun skipSpaces(text: String, start: Int): Int {
    var i = start
    while (i < text.length && text[i].isWhitespace()) i++
    return i
  }

  private fun readBraced(text: String, openIndex: Int): Pair<String, Int>? {
    if (openIndex >= text.length || text[openIndex] != '{') return null
    var depth = 0
    var i = openIndex
    while (i < text.length) {
      when (text[i]) {
        '{' -> depth++
        '}' -> {
          depth--
          if (depth == 0) {
            return text.substring(openIndex + 1, i) to (i + 1)
          }
        }
      }
      i++
    }
    return null
  }

  private fun replaceScripts(input: String, isSuper: Boolean): String {
    val trigger = if (isSuper) '^' else '_'
    val sb = StringBuilder()
    var i = 0
    while (i < input.length) {
      val c = input[i]
      if (c == trigger && i + 1 < input.length) {
        if (input[i + 1] == '{') {
          val braced = readBraced(input, i + 1)
          if (braced != null) {
            val content = braced.first.trim()
            val mapped = if (isSuper) toSuperscript(content) else toSubscript(content)
            sb.append(mapped)
            i = braced.second
            continue
          }
        } else {
          val nextChar = input[i + 1]
          if (nextChar.isLetterOrDigit() || nextChar == '+' || nextChar == '-') {
            val mapped = if (isSuper) SUPERSCRIPTS[nextChar] else SUBSCRIPTS[nextChar]
            if (mapped != null) {
              sb.append(mapped)
              i += 2
              continue
            }
          }
        }
      }
      sb.append(c)
      i++
    }
    return sb.toString()
  }

  private fun toSuperscript(expr: String): String {
    if (expr.isEmpty()) return ""
    val mapped = expr.map { SUPERSCRIPTS[it] }
    return if (mapped.all { it != null }) {
      mapped.joinToString("") { it.toString() }
    } else if (expr.length == 1) {
      "^$expr"
    } else {
      "^($expr)"
    }
  }

  private fun toSubscript(expr: String): String {
    if (expr.isEmpty()) return ""
    val mapped = expr.map { SUBSCRIPTS[it] }
    return if (mapped.all { it != null }) {
      mapped.joinToString("") { it.toString() }
    } else if (expr.length == 1) {
      "_$expr"
    } else {
      "_($expr)"
    }
  }
}

internal object SafeMarkdownParser {
  private val HTML_COMMENT_REGEX = Regex("<!--[\\s\\S]*?-->")
  private val HTML_TAG_REGEX = Regex(
    "</?(?:script|style|iframe|object|embed|svg|img|a|div|span|p|br|hr|b|i|u|strong|em|h[1-6]|ul|ol|li|table|thead|tbody|tr|td|th|form|input|button|select|textarea|link|meta|html|head|body|pre|code|blockquote)(?=\\s|/?>)[^>]*>",
    RegexOption.IGNORE_CASE
  )
  private val SCRIPT_SCHEME_REGEX = Regex("(?i)(javascript:|data:|vbscript:)")
  private val NUMBERED_REGEX = Regex("^(\\d+)\\.\\s+(.*)$")
  private val STEP_HEADING_REGEX = Regex(
    "^(?:#{1,4}\\s*)?(?:\\*\\*)?Step\\s+(\\d+)\\b(?:\\*\\*)?\\s*[:.\\-—–]*\\s*(.*)$",
    RegexOption.IGNORE_CASE
  )
  private val TABLE_DELIMITER_CELL_REGEX = Regex("^:?-{2,}:?$")
  private val DIVIDER_REGEX = Regex("^([-*_])\\1{2,}$")
  private val INLINE_LINK_REGEX = Regex("^\\[([^\\]]+)]\\(([^)\\s]+)\\)")

  fun sanitizeText(raw: String): String {
    if (raw.isEmpty()) return ""
    return raw
      .replace(HTML_COMMENT_REGEX, "")
      .replace(HTML_TAG_REGEX, "")
      .replace(SCRIPT_SCHEME_REGEX, "")
  }

  fun parseBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val rawBlocks = mutableListOf<MarkdownBlock>()

    var i = 0
    while (i < lines.size) {
      val rawLine = lines[i]
      val trimmed = rawLine.trim()

      if (trimmed.isEmpty()) {
        i++
        continue
      }

      // 1. Fenced code block ```lang
      if (trimmed.startsWith("```")) {
        val lang = trimmed.removePrefix("```").trim()
        val codeLines = mutableListOf<String>()
        i++
        while (i < lines.size && !lines[i].trim().startsWith("```")) {
          codeLines.add(lines[i])
          i++
        }
        if (i < lines.size && lines[i].trim().startsWith("```")) i++
        rawBlocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
        continue
      }

      // 2. Display math block $$...$$ or \[...\]
      if (trimmed.startsWith("$$") || trimmed.startsWith("\\[")) {
        val isDollar = trimmed.startsWith("$$")
        val startDelim = if (isDollar) "$$" else "\\["
        val endDelim = if (isDollar) "$$" else "\\]"
        val afterStart = trimmed.removePrefix(startDelim).trim()
        val endOnSameLine = afterStart.indexOf(endDelim)
        if (endOnSameLine >= 0) {
          val latex = afterStart.substring(0, endOnSameLine).trim()
          if (latex.isNotEmpty()) {
            rawBlocks.add(MarkdownBlock.EquationBlock(latex, LatexMathFormatter.format(latex)))
          }
          val trailing = afterStart.substring(endOnSameLine + endDelim.length).trim()
          if (trailing.isNotEmpty()) {
            rawBlocks.add(MarkdownBlock.Paragraph(sanitizeText(trailing)))
          }
          i++
          continue
        } else {
          val eqLines = mutableListOf<String>()
          if (afterStart.isNotEmpty()) eqLines.add(afterStart)
          i++
          while (i < lines.size) {
            val eqLine = lines[i].trim()
            val closeIdx = eqLine.indexOf(endDelim)
            if (closeIdx >= 0) {
              val beforeClose = eqLine.substring(0, closeIdx).trim()
              if (beforeClose.isNotEmpty()) eqLines.add(beforeClose)
              i++
              break
            } else {
              eqLines.add(lines[i].trimEnd())
              i++
            }
          }
          val latex = eqLines.joinToString("\n").trim()
          if (latex.isNotEmpty()) {
            rawBlocks.add(MarkdownBlock.EquationBlock(latex, LatexMathFormatter.format(latex)))
          }
          continue
        }
      }

      // 3. Markdown Table (header + delimiter row)
      if (trimmed.contains('|') && i + 1 < lines.size && isTableDelimiterRow(lines[i + 1].trim())) {
        val headers = splitTableRow(trimmed).map { sanitizeText(it) }
        val alignments = parseTableAlignments(lines[i + 1].trim(), headers.size)
        val tableRawLines = mutableListOf(trimmed, lines[i + 1].trim())
        val rows = mutableListOf<List<String>>()
        i += 2
        while (i < lines.size) {
          val rowTrimmed = lines[i].trim()
          if (rowTrimmed.isEmpty() || !rowTrimmed.contains('|') || isTableDelimiterRow(rowTrimmed)) {
            break
          }
          tableRawLines.add(rowTrimmed)
          val cells = splitTableRow(rowTrimmed).map { sanitizeText(it) }
          val normalized = List(headers.size) { col -> cells.getOrElse(col) { "" } }
          rows.add(normalized)
          i++
        }
        if (headers.isNotEmpty()) {
          rawBlocks.add(
            MarkdownBlock.Table(
              headers = headers,
              alignments = alignments,
              rows = rows,
              rawMarkdown = tableRawLines.joinToString("\n")
            )
          )
        }
        continue
      }

      // 4. Horizontal divider --- / *** / ___
      if (DIVIDER_REGEX.matches(trimmed.replace(" ", ""))) {
        rawBlocks.add(MarkdownBlock.Divider)
        i++
        continue
      }

      // 5. Blockquote > ...
      if (trimmed.startsWith(">")) {
        val quoteLines = mutableListOf<String>()
        while (i < lines.size && lines[i].trim().startsWith(">")) {
          val content = sanitizeText(lines[i].trim().removePrefix(">").trimStart())
          quoteLines.add(content)
          i++
        }
        val cleanQuote = quoteLines.filter { it.isNotBlank() }
        if (cleanQuote.isNotEmpty()) {
          rawBlocks.add(
            MarkdownBlock.Blockquote(
              lines = cleanQuote,
              rawText = cleanQuote.joinToString("\n")
            )
          )
        }
        continue
      }

      // 6. Step N heading / callout
      val cleanTrimmed = sanitizeText(trimmed)
      val stepMatch = STEP_HEADING_REGEX.matchEntire(cleanTrimmed)
      if (stepMatch != null) {
        val stepNum = stepMatch.groupValues[1]
        val title = stepMatch.groupValues[2].trim().removeSuffix("**").trim()
        rawBlocks.add(
          MarkdownBlock.StepBlock(
            stepNumber = stepNum,
            title = title,
            body = ""
          )
        )
        i++
        continue
      }

      // 7. Markdown Heading #..####
      if (cleanTrimmed.startsWith("#")) {
        val hashes = cleanTrimmed.takeWhile { it == '#' }.length
        if (hashes in 1..4 && cleanTrimmed.length > hashes && cleanTrimmed[hashes].isWhitespace()) {
          val headingText = cleanTrimmed.drop(hashes).trim()
          if (headingText.isNotEmpty()) {
            rawBlocks.add(MarkdownBlock.Heading(level = hashes, text = headingText))
            i++
            continue
          }
        }
      }

      // 8. Bullet / Numbered list item with indent awareness
      val leadingSpaces = rawLine.takeWhile { it == ' ' || it == '\t' }.sumOf { if (it == '\t') 4 else 1 }
      val indentLevel = (leadingSpaces / 2).coerceIn(0, 3)

      val numberedMatch = NUMBERED_REGEX.matchEntire(cleanTrimmed)
      when {
        cleanTrimmed.startsWith("- ") || cleanTrimmed.startsWith("* ") || cleanTrimmed.startsWith("• ") -> {
          rawBlocks.add(MarkdownBlock.Bullet(cleanTrimmed.drop(2).trim(), indentLevel))
          i++
          continue
        }
        numberedMatch != null -> {
          rawBlocks.add(
            MarkdownBlock.Numbered(
              numberLabel = "${numberedMatch.groupValues[1]}.",
              text = numberedMatch.groupValues[2].trim(),
              indentLevel = indentLevel
            )
          )
          i++
          continue
        }
      }

      // 9. Merge consecutive prose lines into a clean paragraph
      val paraLines = mutableListOf(cleanTrimmed)
      i++
      while (i < lines.size) {
        val nextRaw = lines[i]
        val nextTrimmed = nextRaw.trim()
        if (nextTrimmed.isEmpty() || isBlockStarter(nextTrimmed, lines.getOrNull(i + 1)?.trim())) {
          break
        }
        paraLines.add(sanitizeText(nextTrimmed))
        i++
      }
      rawBlocks.add(MarkdownBlock.Paragraph(paraLines.joinToString(" ")))
    }

    return attachSectionCopyText(rawBlocks)
  }

  private fun isBlockStarter(trimmed: String, nextTrimmed: String?): Boolean {
    if (trimmed.startsWith("```") ||
      trimmed.startsWith("$$") ||
      trimmed.startsWith("\\[") ||
      trimmed.startsWith(">") ||
      trimmed.startsWith("#") ||
      trimmed.startsWith("- ") ||
      trimmed.startsWith("* ") ||
      trimmed.startsWith("• ") ||
      DIVIDER_REGEX.matches(trimmed.replace(" ", "")) ||
      NUMBERED_REGEX.matches(trimmed) ||
      STEP_HEADING_REGEX.matches(trimmed)
    ) {
      return true
    }
    return trimmed.contains('|') && nextTrimmed != null && isTableDelimiterRow(nextTrimmed)
  }

  private fun attachSectionCopyText(blocks: List<MarkdownBlock>): List<MarkdownBlock> {
    if (blocks.none { it is MarkdownBlock.Heading && it.level <= 2 }) return blocks
    return blocks.mapIndexed { idx, block ->
      if (block is MarkdownBlock.Heading && block.level <= 2) {
        val sectionLines = mutableListOf(block.text)
        for (j in (idx + 1) until blocks.size) {
          val next = blocks[j]
          if (next is MarkdownBlock.Heading && next.level <= block.level) break
          when (next) {
            is MarkdownBlock.Heading -> sectionLines.add(next.text)
            is MarkdownBlock.Paragraph -> sectionLines.add(next.text)
            is MarkdownBlock.Bullet -> sectionLines.add("• ${next.text}")
            is MarkdownBlock.Numbered -> sectionLines.add("${next.numberLabel} ${next.text}")
            is MarkdownBlock.StepBlock -> sectionLines.add(
              listOfNotNull(
                "Step ${next.stepNumber}${if (next.title.isNotBlank()) ": ${next.title}" else ""}",
                next.body.takeIf { it.isNotBlank() }
              ).joinToString("\n")
            )
            is MarkdownBlock.CodeBlock -> sectionLines.add("```${next.language}\n${next.code}\n```")
            is MarkdownBlock.Table -> sectionLines.add(next.rawMarkdown)
            is MarkdownBlock.EquationBlock -> sectionLines.add(next.rawLatex)
            is MarkdownBlock.Blockquote -> sectionLines.add(next.rawText)
            MarkdownBlock.Divider -> Unit
          }
        }
        val copyPayload = if (sectionLines.size > 1) sectionLines.joinToString("\n\n") else null
        block.copy(sectionCopyText = copyPayload)
      } else {
        block
      }
    }
  }

  internal fun isTableDelimiterRow(line: String): Boolean {
    if (!line.contains('|') || !line.contains('-')) return false
    val cells = splitTableRow(line)
    return cells.isNotEmpty() && cells.all { TABLE_DELIMITER_CELL_REGEX.matches(it.trim()) }
  }

  internal fun splitTableRow(line: String): List<String> {
    var trimmed = line.trim()
    if (trimmed.startsWith("|")) trimmed = trimmed.drop(1)
    if (trimmed.endsWith("|")) trimmed = trimmed.dropLast(1)
    if (trimmed.isEmpty()) return emptyList()
    return trimmed.split('|').map { it.trim() }
  }

  private fun parseTableAlignments(delimiterLine: String, columnCount: Int): List<TableAlignment> {
    val cells = splitTableRow(delimiterLine)
    return List(columnCount) { idx ->
      val cell = cells.getOrElse(idx) { "---" }.trim()
      val left = cell.startsWith(":")
      val right = cell.endsWith(":")
      when {
        left && right -> TableAlignment.CENTER
        right -> TableAlignment.RIGHT
        else -> TableAlignment.LEFT
      }
    }
  }

  fun parseInlineMarkdown(
    text: String,
    boldColor: Color,
    codeColor: Color,
    codeBgColor: Color,
    linkColor: Color,
    mathColor: Color
  ): AnnotatedString {
    val safe = sanitizeText(text)
    return buildAnnotatedString {
      var idx = 0
      while (idx < safe.length) {
        // 1. Inline math \( ... \)
        if (safe.startsWith("\\(", idx)) {
          val end = safe.indexOf("\\)", idx + 2)
          if (end > idx + 2) {
            val formatted = LatexMathFormatter.format(safe.substring(idx + 2, end))
            pushStyle(SpanStyle(color = mathColor, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium))
            append(formatted)
            pop()
            idx = end + 2
            continue
          }
        }

        // 2. Inline math $...$ (non-currency heuristic: avoid $5 or $100 followed by space/digit)
        if (safe[idx] == '$' && (idx + 1 < safe.length && safe[idx + 1] != '$' && !safe[idx + 1].isWhitespace())) {
          val end = findClosingInlineMathDollar(safe, idx + 1)
          if (end > idx + 1) {
            val expr = safe.substring(idx + 1, end)
            val looksCurrency = expr.all { it.isDigit() || it == '.' || it == ',' }
            if (!looksCurrency) {
              val formatted = LatexMathFormatter.format(expr)
              pushStyle(SpanStyle(color = mathColor, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium))
              append(formatted)
              pop()
              idx = end + 1
              continue
            }
          }
        }

        // 3. Inline code `code`
        if (safe[idx] == '`') {
          val end = safe.indexOf('`', idx + 1)
          if (end > idx + 1) {
            pushStyle(
              SpanStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.5.sp,
                color = codeColor,
                background = codeBgColor
              )
            )
            append(" ${safe.substring(idx + 1, end)} ")
            pop()
            idx = end + 1
            continue
          }
        }

        // 4. Bold + Italic ***text***
        if (safe.startsWith("***", idx)) {
          val end = safe.indexOf("***", idx + 3)
          if (end > idx + 3) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = boldColor))
            append(safe.substring(idx + 3, end))
            pop()
            idx = end + 3
            continue
          }
        }

        // 5. Bold **text** or __text__
        if (safe.startsWith("**", idx) || safe.startsWith("__", idx)) {
          val delim = safe.substring(idx, idx + 2)
          val end = safe.indexOf(delim, idx + 2)
          if (end > idx + 2) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor))
            append(parseInlineMarkdown(safe.substring(idx + 2, end), boldColor, codeColor, codeBgColor, linkColor, mathColor))
            pop()
            idx = end + 2
            continue
          }
        }

        // 6. Strikethrough ~~text~~
        if (safe.startsWith("~~", idx)) {
          val end = safe.indexOf("~~", idx + 2)
          if (end > idx + 2) {
            pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
            append(safe.substring(idx + 2, end))
            pop()
            idx = end + 2
            continue
          }
        }

        // 7. Italic *text* or _text_ (word-boundary safe for underscores in snake_case)
        if (safe[idx] == '*' || (safe[idx] == '_' && (idx == 0 || !safe[idx - 1].isLetterOrDigit()))) {
          val delim = safe[idx]
          val end = findClosingItalic(safe, idx + 1, delim)
          if (end > idx + 1) {
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(safe.substring(idx + 1, end))
            pop()
            idx = end + 1
            continue
          }
        }

        // 8. Markdown link [label](url)
        if (safe[idx] == '[') {
          val linkMatch = INLINE_LINK_REGEX.find(safe.substring(idx))
          if (linkMatch != null) {
            val label = linkMatch.groupValues[1]
            val url = linkMatch.groupValues[2].trim()
            if (url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)) {
              pushLink(
                LinkAnnotation.Url(
                  url = url,
                  styles = TextLinkStyles(
                    style = SpanStyle(
                      color = linkColor,
                      textDecoration = TextDecoration.Underline,
                      fontWeight = FontWeight.Medium
                    )
                  )
                )
              )
              append(label)
              pop()
            } else {
              append(label)
            }
            idx += linkMatch.value.length
            continue
          }
        }

        append(safe[idx])
        idx++
      }
    }
  }

  private fun findClosingInlineMathDollar(text: String, start: Int): Int {
    var i = start
    while (i < text.length) {
      if (text[i] == '$' && (i == 0 || text[i - 1] != '\\') && !text[i - 1].isWhitespace()) {
        return i
      }
      if (text[i] == '\n') return -1
      i++
    }
    return -1
  }

  private fun findClosingItalic(text: String, start: Int, delim: Char): Int {
    var i = start
    while (i < text.length) {
      if (text[i] == delim) {
        val after = text.getOrNull(i + 1)
        val validBoundary = delim == '*' || after == null || !after.isLetterOrDigit()
        if (validBoundary && !text[i - 1].isWhitespace()) {
          return i
        }
      }
      i++
    }
    return -1
  }
}
