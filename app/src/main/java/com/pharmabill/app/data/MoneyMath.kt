package com.pharmabill.app.data

import kotlin.math.roundToLong

/** Pure billing maths — no Android types, so the rules are easy to reason
 *  about and to test. All money is integer paise (Long). Dates are
 *  yyyy-MM-dd strings, which compare correctly as plain strings. */

/** GST-inclusive line total: MRP already includes GST, so the line total
 *  is simply MRP x quantity. No Double anywhere in the chain. */
fun lineTotalPaise(mrpPaise: Long, qty: Int): Long = mrpPaise * qty

/**
 * GST embedded in a GST-inclusive amount: gst = total - total*100/(100+rate),
 * rounded to the nearest paise. Split into equal CGST/SGST halves; an odd
 * paise goes to CGST so cgst + sgst always equals the total GST.
 */
data class GstBreakup(val taxablePaise: Long, val cgstPaise: Long, val sgstPaise: Long)

fun gstBreakup(inclusivePaise: Long, gstPercent: Int): GstBreakup {
    if (gstPercent <= 0) return GstBreakup(inclusivePaise, 0, 0)
    val taxable = (inclusivePaise * 100.0 / (100 + gstPercent)).roundToLong()
    val gst = inclusivePaise - taxable
    val cgst = (gst + 1) / 2
    return GstBreakup(taxable, cgst, gst - cgst)
}

/** Bill numbers run PB-0001, PB-0002, ... in insertion order. */
fun billNo(countSoFar: Int): String = "PB-%04d".format(countSoFar + 1)

/** Expiry buckets used by the Dashboard alerts. [daysUntil] is negative
 *  for already-expired batches. */
enum class ExpiryBucket { EXPIRED, WITHIN_30, WITHIN_90, OK }

fun expiryBucket(expiryIso: String, todayIso: String): ExpiryBucket {
    val days = daysBetween(todayIso, expiryIso)
    return when {
        days < 0 -> ExpiryBucket.EXPIRED
        days <= 30 -> ExpiryBucket.WITHIN_30
        days <= 90 -> ExpiryBucket.WITHIN_90
        else -> ExpiryBucket.OK
    }
}

/** Whole days from [fromIso] to [toIso] (negative when [toIso] is past).
 *  Returns a large number for unparseable input so bad dates sort as OK
 *  rather than falsely alarming. */
fun daysBetween(fromIso: String, toIso: String): Long {
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val from = fmt.parse(fromIso)?.time ?: return Long.MAX_VALUE
        val to = fmt.parse(toIso)?.time ?: return Long.MAX_VALUE
        (to - from) / 86_400_000L
    } catch (e: Exception) {
        Long.MAX_VALUE
    }
}

/** Parse a rupees string ("12.50", "12") into paise, or null when invalid. */
fun parseRupeesToPaise(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return try {
        val bd = java.math.BigDecimal(trimmed)
        if (bd < java.math.BigDecimal.ZERO) null
        else bd.multiply(java.math.BigDecimal(100))
            .setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
    } catch (e: Exception) {
        null
    }
}
