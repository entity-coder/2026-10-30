package com.pharmabill.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved bill. [subtotalPaise] is the sum of line totals (MRP x qty,
 * GST-inclusive), [gstPaise] is the GST embedded in that subtotal, split
 * equally into CGST and SGST for display. All money is integer paise.
 */
@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNo: String,
    val date: String,
    val customerName: String?,
    val subtotalPaise: Long,
    val gstPaise: Long,
    val totalPaise: Long
)

/** One line on a bill. Medicine name/batch are copied in at sale time so
 *  the invoice never changes if the catalog row is later edited. */
@Entity(
    tableName = "bill_lines",
    foreignKeys = [ForeignKey(
        entity = Bill::class, parentColumns = ["id"], childColumns = ["billId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("billId"), Index("medicineId")]
)
data class BillLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val medicineId: Long,
    val medicineName: String,
    val batch: String,
    val qty: Int,
    val mrpPaise: Long,
    val gstPercent: Int,
    val lineTotalPaise: Long
)
