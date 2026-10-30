package com.pharmabill.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Append-only stock log: every stock change (manual in/out, or a sale)
 * writes one row here. Rows are never edited or deleted, so a medicine's
 * movement history is a trustworthy audit trail. [delta] is negative for
 * out-movements (sales, adjustments down) and positive for in-movements.
 */
@Entity(tableName = "stock_log")
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val batch: String,
    val delta: Int,
    val reason: String,
    val date: String
)
