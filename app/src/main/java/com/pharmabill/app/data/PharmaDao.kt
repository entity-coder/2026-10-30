package com.pharmabill.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PharmaDao {
    // ---- medicines ----
    /** Active catalog, earliest-expiry first within each name — the
     *  billing picker relies on this ordering for FEFO selling. */
    @Query("SELECT * FROM medicines WHERE active = 1 ORDER BY name COLLATE NOCASE ASC, expiry ASC")
    fun medicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun medicineById(id: Long): Medicine?

    @Query("SELECT * FROM medicines WHERE active = 1 AND (name LIKE '%' || :q || '%' OR batch LIKE '%' || :q || '%') ORDER BY name COLLATE NOCASE ASC, expiry ASC")
    fun searchMedicines(q: String): Flow<List<Medicine>>

    @Insert
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    /** How many bill lines reference this batch — the delete guard. */
    @Query("SELECT COUNT(*) FROM bill_lines WHERE medicineId = :id")
    suspend fun billLineCountFor(id: Long): Int

    // ---- bills ----
    @Query("SELECT * FROM bills ORDER BY id DESC")
    fun bills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE date = :date ORDER BY id DESC")
    fun billsForDate(date: String): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun billById(id: Long): Bill?

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun billCount(): Int

    @Insert
    suspend fun insertBill(bill: Bill): Long

    @Insert
    suspend fun insertBillLines(lines: List<BillLine>)

    @Query("SELECT * FROM bill_lines WHERE billId = :billId ORDER BY id ASC")
    fun linesForBill(billId: Long): Flow<List<BillLine>>

    // ---- stock log ----
    @Insert
    suspend fun insertMovement(movement: StockMovement)

    @Query("SELECT * FROM stock_log WHERE medicineId = :medicineId ORDER BY id DESC")
    fun movementsFor(medicineId: Long): Flow<List<StockMovement>>
}
