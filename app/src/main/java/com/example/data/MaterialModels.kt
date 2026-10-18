package com.example.data

data class MaterialItem(
  val id: String,
  val title: String,
  val description: String,
  val materialType: String, // "notes", "slides", "past_paper", "video", "link"
  val externalUrl: String,
  val createdAt: String
) {
  val typeLabel: String
    get() = when (materialType.lowercase()) {
      "notes" -> "Notes"
      "slides" -> "Slides"
      "past_paper" -> "Past Paper"
      "video" -> "Video"
      "link" -> "Link"
      else -> materialType.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

  val formattedDate: String
    get() {
      if (createdAt.isBlank()) return ""
      return try {
        createdAt.take(10)
      } catch (_: Exception) {
        createdAt
      }
    }
}
