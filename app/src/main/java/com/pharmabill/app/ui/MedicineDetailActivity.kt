package com.pharmabill.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.pharmabill.app.R
import com.pharmabill.app.data.Medicine
import com.pharmabill.app.data.PharmaDatabase
import com.pharmabill.app.data.StockMovement
import com.pharmabill.app.data.fmtGst
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.data.todayIso
import com.pharmabill.app.databinding.ActivityMedicineDetailBinding
import com.pharmabill.app.databinding.DialogStockAdjustBinding
import kotlinx.coroutines.launch

/**
 * One medicine batch: details, stock in/out with a reason (each write
 * also appends a row to the append-only stock log), the movement history,
 * and the delete guard — a batch that appears on any bill cannot be
 * deleted, only deactivated, so old invoices stay intact.
 */
class MedicineDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_MEDICINE_ID = "medicine_id"
    }

    private lateinit var binding: ActivityMedicineDetailBinding
    private var medicine: Medicine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicineDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dao = PharmaDatabase.get(this).dao()
        val medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1)
        val movementAdapter = MovementAdapter()
        binding.movementList.layoutManager = LinearLayoutManager(this)
        binding.movementList.adapter = movementAdapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.movementsFor(medicineId).collect { movementAdapter.submit(it) }
            }
        }
        lifecycleScope.launch {
            medicine = dao.medicineById(medicineId)
            val med = medicine
            if (med == null) {
                Toast.makeText(this@MedicineDetailActivity,
                    R.string.medicine_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            binding.detailName.text = med.name
            binding.detailMeta.text =
                "Batch ${med.batch} · Exp ${med.expiry} · " +
                "GST ${fmtGst(med.gstPercent)}" +
                (med.company?.let { " · $it" } ?: "")
            binding.detailStock.text =
                "Stock: ${med.stock} · MRP ${fmtINR(med.mrpPaise)} · " +
                "Low-stock threshold: ${med.lowStockThreshold}"
            binding.detailActive.visibility =
                if (med.active) View.GONE else View.VISIBLE
        }
        binding.stockInButton.setOnClickListener { showAdjustDialog(dao, true) }
        binding.stockOutButton.setOnClickListener { showAdjustDialog(dao, false) }
        binding.editButton.setOnClickListener {
            val med = medicine ?: return@setOnClickListener
            showMedicineDialog(this, lifecycleScope, dao, med)
        }
        binding.deleteButton.setOnClickListener {
            val med = medicine ?: return@setOnClickListener
            lifecycleScope.launch {
                val billed = dao.billLineCountFor(med.id)
                if (billed > 0) {
                    // Delete guard: never remove a batch that invoices
                    // reference — offer deactivation instead.
                    AlertDialog.Builder(this@MedicineDetailActivity)
                        .setTitle(R.string.cannot_delete_medicine)
                        .setMessage(getString(
                            R.string.cannot_delete_medicine_message, billed))
                        .setPositiveButton(R.string.deactivate) { _, _ ->
                            lifecycleScope.launch {
                                dao.updateMedicine(med.copy(active = false))
                                Toast.makeText(this@MedicineDetailActivity,
                                    R.string.medicine_deactivated,
                                    Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else {
                    AlertDialog.Builder(this@MedicineDetailActivity)
                        .setTitle(R.string.delete_medicine)
                        .setMessage(R.string.delete_medicine_message)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            lifecycleScope.launch {
                                dao.deleteMedicine(med)
                                finish()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
        }
    }

    private fun showAdjustDialog(
        dao: com.pharmabill.app.data.PharmaDao, isIn: Boolean
    ) {
        val dialogBinding = DialogStockAdjustBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setTitle(if (isIn) R.string.stock_in else R.string.stock_out)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.record) { _, _ ->
                val med = medicine ?: return@setPositiveButton
                val qty = dialogBinding.qtyInput.text.toString().toIntOrNull()
                val reason = dialogBinding.reasonInput.text.toString().trim()
                if (qty == null || qty <= 0 || reason.isEmpty()) {
                    Toast.makeText(this, R.string.stock_adjust_invalid,
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val delta = if (isIn) qty else -qty
                if (!isIn && qty > med.stock) {
                    Toast.makeText(this, R.string.stock_out_exceeds,
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val current = dao.medicineById(med.id) ?: return@launch
                    dao.updateMedicine(current.copy(stock = current.stock + delta))
                    dao.insertMovement(
                        StockMovement(
                            medicineId = current.id,
                            medicineName = current.name,
                            batch = current.batch,
                            delta = delta,
                            reason = reason,
                            date = todayIso()
                        )
                    )
                    // Refresh the cached row so the header shows new stock.
                    medicine = dao.medicineById(med.id)
                    medicine?.let {
                        binding.detailStock.text =
                            "Stock: ${it.stock} · MRP ${fmtINR(it.mrpPaise)} · " +
                            "Low-stock threshold: ${it.lowStockThreshold}"
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
