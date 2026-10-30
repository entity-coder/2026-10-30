package com.pharmabill.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One medicine batch in the catalog. A medicine name can appear several
 * times (one row per batch) — that is deliberate: expiry and MRP are
 * batch properties, and the billing picker sells the earliest-expiry
 * batch first.
 *
 * Money is stored as integer paise (Long), never Double, so totals never
 * accumulate floating-point error. [expiry] is a yyyy-MM-dd string, which
 * sorts lexicographically in the same order as chronologically.
 */
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val batch: String,
    val company: String?,
    val expiry: String,
    val mrpPaise: Long,
    val gstPercent: Int,
    val stock: Int,
    val lowStockThreshold: Int,
    /** False = deactivated ("hidden"), never hard-deleted once billed. */
    val active: Boolean = true
)
