package com.pharmabill.app.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.pharmabill.app.R
import com.pharmabill.app.data.Medicine
import com.pharmabill.app.data.PharmaDao
import com.pharmabill.app.data.parseRupeesToPaise
import com.pharmabill.app.databinding.DialogMedicineBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shared add/edit medicine dialog, used from both the catalog list and
 * the detail screen. Validation rejects empty name/batch, a malformed
 * expiry date, non-positive MRP, negative GST/stock/threshold.
 */
fun showMedicineDialog(
    context: Context,
    scope: CoroutineScope,
    dao: PharmaDao,
    existing: Medicine?
) {
    val dialogBinding = DialogMedicineBinding.inflate(
        LayoutInflater.from(context)
    )
    existing?.let {
        dialogBinding.nameInput.setText(it.name)
        dialogBinding.batchInput.setText(it.batch)
        dialogBinding.companyInput.setText(it.company ?: "")
        dialogBinding.expiryInput.setText(it.expiry)
        dialogBinding.mrpInput.setText("%.2f".format(it.mrpPaise / 100.0))
        dialogBinding.gstInput.setText(it.gstPercent.toString())
        dialogBinding.stockInput.setText(it.stock.toString())
        dialogBinding.thresholdInput.setText(it.lowStockThreshold.toString())
    }
    AlertDialog.Builder(context)
        .setTitle(if (existing == null) R.string.add_medicine else R.string.edit_medicine)
        .setView(dialogBinding.root)
        .setPositiveButton(R.string.save) { _, _ ->
            val name = dialogBinding.nameInput.text.toString().trim()
            val batch = dialogBinding.batchInput.text.toString().trim()
            val company = dialogBinding.companyInput.text.toString().trim()
                .ifEmpty { null }
            val expiry = dialogBinding.expiryInput.text.toString().trim()
            val mrp = parseRupeesToPaise(dialogBinding.mrpInput.text.toString())
            val gst = dialogBinding.gstInput.text.toString().toIntOrNull()
            val stock = dialogBinding.stockInput.text.toString().toIntOrNull()
            val threshold = dialogBinding.thresholdInput.text.toString()
                .toIntOrNull()
            if (name.isEmpty() || batch.isEmpty() ||
                !expiry.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ||
                mrp == null || mrp <= 0 || gst == null || gst < 0 ||
                stock == null || stock < 0 || threshold == null || threshold < 0
            ) {
                Toast.makeText(context, R.string.medicine_invalid,
                    Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            scope.launch {
                if (existing == null) {
                    dao.insertMedicine(
                        Medicine(
                            name = name, batch = batch, company = company,
                            expiry = expiry, mrpPaise = mrp, gstPercent = gst,
                            stock = stock, lowStockThreshold = threshold
                        )
                    )
                } else {
                    dao.updateMedicine(
                        existing.copy(
                            name = name, batch = batch, company = company,
                            expiry = expiry, mrpPaise = mrp, gstPercent = gst,
                            stock = stock, lowStockThreshold = threshold
                        )
                    )
                }
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
