package com.example.admin.util

import org.json.JSONObject

object AdminJsonHelper {
  fun str(obj: JSONObject?, key: String): String {
    if (obj == null || !obj.has(key) || obj.isNull(key)) return ""
    val raw = obj.optString(key, "").trim()
    return if (raw.equals("null", ignoreCase = true)) "" else raw
  }

  fun strOrNull(obj: JSONObject?, key: String): String? {
    val value = str(obj, key)
    return value.ifBlank { null }
  }

  fun firstStr(obj: JSONObject?, vararg keys: String): String {
    for (k in keys) {
      val v = str(obj, k)
      if (v.isNotBlank()) return v
    }
    return ""
  }

  fun bool(obj: JSONObject?, key: String, default: Boolean = false): Boolean {
    if (obj == null || !obj.has(key) || obj.isNull(key)) return default
    return obj.optBoolean(key, default)
  }

  fun intVal(obj: JSONObject?, key: String, default: Int = 0): Int {
    if (obj == null || !obj.has(key) || obj.isNull(key)) return default
    return obj.optInt(key, default)
  }

  fun orDash(value: String?): String {
    val v = value?.trim().orEmpty()
    return if (v.isBlank() || v.equals("null", ignoreCase = true)) "-" else v
  }
}
