package com.example.ui

import com.example.data.ExamSubmissionRow
import com.example.data.MarkingStatus

enum class SubmissionSortOrder(val label: String) {
  NAME_ASC("Name (A–Z)"),
  ADMISSION_ASC("Admission No.")
}

object ExamSubmissionSorter {
  private val NATURAL_TOKEN_REGEX = Regex("\\d+|\\D+")

  fun sortAndFilter(
    submissions: List<ExamSubmissionRow>,
    filter: SubmissionFilter,
    sortOrder: SubmissionSortOrder,
    waitingFirst: Boolean,
    searchQuery: String
  ): List<ExamSubmissionRow> {
    val cleanQuery = searchQuery.trim().lowercase()
    return submissions
      .asSequence()
      .filter { row ->
        when (filter) {
          SubmissionFilter.ALL -> true
          SubmissionFilter.WAITING -> row.markingStatus == MarkingStatus.PENDING
          SubmissionFilter.MARKING -> row.markingStatus == MarkingStatus.MARKING
          SubmissionFilter.RETURNED -> row.markingStatus == MarkingStatus.RELEASED
        }
      }
      .filter { row ->
        cleanQuery.isEmpty() ||
          row.fullName.lowercase().contains(cleanQuery) ||
          row.admissionNumber.lowercase().contains(cleanQuery)
      }
      .sortedWith(comparator(sortOrder, waitingFirst))
      .toList()
  }

  private fun comparator(
    sortOrder: SubmissionSortOrder,
    waitingFirst: Boolean
  ): Comparator<ExamSubmissionRow> = Comparator { a, b ->
    if (waitingFirst) {
      val rankCmp = statusRank(a.markingStatus).compareTo(statusRank(b.markingStatus))
      if (rankCmp != 0) return@Comparator rankCmp
    }
    when (sortOrder) {
      SubmissionSortOrder.NAME_ASC -> {
        val nameCmp = compareText(a.fullName, b.fullName)
        if (nameCmp != 0) return@Comparator nameCmp
        val admCmp = compareNatural(a.admissionNumber, b.admissionNumber)
        if (admCmp != 0) return@Comparator admCmp
      }
      SubmissionSortOrder.ADMISSION_ASC -> {
        val admCmp = compareNatural(a.admissionNumber, b.admissionNumber)
        if (admCmp != 0) return@Comparator admCmp
        val nameCmp = compareText(a.fullName, b.fullName)
        if (nameCmp != 0) return@Comparator nameCmp
      }
    }
    a.studentId.compareTo(b.studentId)
  }

  private fun statusRank(status: MarkingStatus): Int = when (status) {
    MarkingStatus.PENDING -> 0
    MarkingStatus.MARKING -> 1
    MarkingStatus.RELEASED -> 2
    MarkingStatus.AUTO -> 3
  }

  private fun compareText(a: String, b: String): Int {
    val ca = a.trim().lowercase()
    val cb = b.trim().lowercase()
    val aMissing = ca.isEmpty() || ca == "-"
    val bMissing = cb.isEmpty() || cb == "-"
    if (aMissing != bMissing) return if (aMissing) 1 else -1
    return ca.compareTo(cb)
  }

  fun compareNatural(a: String, b: String): Int {
    val ca = a.trim().lowercase()
    val cb = b.trim().lowercase()
    val aMissing = ca.isEmpty() || ca == "-"
    val bMissing = cb.isEmpty() || cb == "-"
    if (aMissing != bMissing) return if (aMissing) 1 else -1

    val tokensA = NATURAL_TOKEN_REGEX.findAll(ca).map { it.value }.toList()
    val tokensB = NATURAL_TOKEN_REGEX.findAll(cb).map { it.value }.toList()
    val minLen = minOf(tokensA.size, tokensB.size)

    for (i in 0 until minLen) {
      val ta = tokensA[i]
      val tb = tokensB[i]
      if (ta == tb) continue
      val aIsNum = ta.all { it.isDigit() }
      val bIsNum = tb.all { it.isDigit() }
      if (aIsNum && bIsNum) {
        val na = ta.trimStart('0').ifEmpty { "0" }
        val nb = tb.trimStart('0').ifEmpty { "0" }
        if (na.length != nb.length) return na.length.compareTo(nb.length)
        val numCmp = na.compareTo(nb)
        if (numCmp != 0) return numCmp
        // Shorter leading-zero form first if numeric values are equal
        val lenCmp = ta.length.compareTo(tb.length)
        if (lenCmp != 0) return lenCmp
      } else {
        val cmp = ta.compareTo(tb)
        if (cmp != 0) return cmp
      }
    }
    return tokensA.size.compareTo(tokensB.size)
  }
}
