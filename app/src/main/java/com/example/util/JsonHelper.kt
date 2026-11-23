package com.example.util

import org.json.JSONObject

object JsonHelper {
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

  fun doubleVal(obj: JSONObject?, key: String, default: Double = 0.0): Double {
    if (obj == null || !obj.has(key) || obj.isNull(key)) return default
    val v = obj.optDouble(key, Double.NaN)
    if (!v.isNaN()) return v
    return obj.optString(key, "").trim().toDoubleOrNull() ?: default
  }

  fun doubleOrNull(obj: JSONObject?, key: String): Double? {
    if (obj == null || !obj.has(key) || obj.isNull(key)) return null
    val raw = obj.opt(key) ?: return null
    if (raw === JSONObject.NULL) return null
    val d = obj.optDouble(key, Double.NaN)
    if (!d.isNaN()) return d
    return obj.optString(key, "").trim().toDoubleOrNull()
  }

  fun orDash(value: String?): String {
    val v = value?.trim().orEmpty()
    return if (v.isBlank() || v.equals("null", ignoreCase = true)) "-" else v
  }
}
