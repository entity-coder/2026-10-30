package com.pharmabill.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.pharmabill.app.R
import com.pharmabill.app.data.Bill
import com.pharmabill.app.data.BillLine
import com.pharmabill.app.data.Medicine
import com.pharmabill.app.data.PharmaDatabase
import com.pharmabill.app.data.StockMovement
import com.pharmabill.app.data.billNo
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.data.gstBreakup
import com.pharmabill.app.data.lineTotalPaise
import com.pharmabill.app.data.todayIso
import com.pharmabill.app.databinding.DialogPickMedicineBinding
import com.pharmabill.app.databinding.FragmentBillingBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One cart line: the chosen batch plus a quantity. Kept in memory only
 *  until the bill is saved — an unsaved cart is intentionally discarded
 *  when leaving the screen. */
data class CartLine(val medicine: Medicine, val qty: Int)

/**
 * Fast billing: pick batches (earliest-expiry first), quantities capped
 * at available stock, GST-inclusive totals with a CGST/SGST breakup.
 * Saving a bill deducts stock and writes one stock-log row per line.
 */
class BillingFragment : Fragment() {
    private var _binding: FragmentBillingBinding? = null
    private val binding get() = _binding!!
    private val cart = MutableStateFlow<List<CartLine>>(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dao = PharmaDatabase.get(requireContext()).dao()
        val adapter = BillingLineAdapter(
            onQtyChange = { line, newQty ->
                cart.value = cart.value.map {
                    if (it.medicine.id == line.medicine.id) it.copy(qty = newQty)
                    else it
                }.filter { it.qty > 0 }
            },
            onRemove = { line ->
                cart.value = cart.value.filter { it.medicine.id != line.medicine.id }
            }
        )
        binding.cartList.layoutManager = LinearLayoutManager(requireContext())
        binding.cartList.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cart.collect { lines ->
                    adapter.submit(lines)
                    val subtotal = lines.sumOf {
                        lineTotalPaise(it.medicine.mrpPaise, it.qty)
                    }
                    var taxable = 0L
                    var cgst = 0L
                    var sgst = 0L
                    lines.forEach {
                        val breakup = gstBreakup(
                            lineTotalPaise(it.medicine.mrpPaise, it.qty),
                            it.medicine.gstPercent
                        )
                        taxable += breakup.taxablePaise
                        cgst += breakup.cgstPaise
                        sgst += breakup.sgstPaise
                    }
                    binding.subtotalValue.text = fmtINR(subtotal)
                    binding.taxableValue.text = fmtINR(taxable)
                    binding.cgstValue.text = fmtINR(cgst)
                    binding.sgstValue.text = fmtINR(sgst)
                    binding.totalValue.text = fmtINR(subtotal)
                    binding.saveButton.isEnabled = lines.isNotEmpty()
                    binding.emptyCart.visibility =
                        if (lines.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        binding.pickButton.setOnClickListener { showPickerDialog(dao) }
        binding.saveButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val lines = cart.value
                if (lines.isEmpty()) return@launch
                // Re-read stock at save time: refuse the whole bill if
                // any line now exceeds available stock (over-sell guard).
                val fresh = lines.mapNotNull { line ->
                    dao.medicineById(line.medicine.id)?.let { line to it }
                }
                val overSold = fresh.firstOrNull { (line, med) ->
                    line.qty > med.stock
                }
                if (overSold != null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.oversell, overSold.second.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                val subtotal = lines.sumOf {
                    lineTotalPaise(it.medicine.mrpPaise, it.qty)
                }
                val gstTotal = lines.sumOf {
                    val lineTotal = lineTotalPaise(it.medicine.mrpPaise, it.qty)
                    lineTotal - gstBreakup(lineTotal, it.medicine.gstPercent)
                        .taxablePaise
                }
                val number = billNo(dao.billCount())
                val customer = binding.customerInput.text.toString().trim()
                    .ifEmpty { null }
                val billId = dao.insertBill(
                    Bill(
                        billNo = number,
                        date = todayIso(),
                        customerName = customer,
                        subtotalPaise = subtotal,
                        gstPaise = gstTotal,
                        totalPaise = subtotal
                    )
                )
                dao.insertBillLines(lines.map {
                    BillLine(
                        billId = billId,
                        medicineId = it.medicine.id,
                        medicineName = it.medicine.name,
                        batch = it.medicine.batch,
                        qty = it.qty,
                        mrpPaise = it.medicine.mrpPaise,
                        gstPercent = it.medicine.gstPercent,
                        lineTotalPaise = lineTotalPaise(it.medicine.mrpPaise, it.qty)
                    )
                })
                fresh.forEach { (line, med) ->
                    dao.updateMedicine(med.copy(stock = med.stock - line.qty))
                    dao.insertMovement(
                        StockMovement(
                            medicineId = med.id,
                            medicineName = med.name,
                            batch = med.batch,
                            delta = -line.qty,
                            reason = "Sale $number",
                            date = todayIso()
                        )
                    )
                }
                cart.value = emptyList()
                binding.customerInput.setText("")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.bill_saved, number),
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(
                    Intent(requireContext(), BillDetailActivity::class.java)
                        .putExtra(BillDetailActivity.EXTRA_BILL_ID, billId)
                )
            }
        }
    }

    /** Picker: active batches with stock, earliest-expiry first within a
     *  name (the DAO ordering), filtered by the search box. Tapping a row
     *  adds one unit, capped at available stock. */
    private fun showPickerDialog(dao: com.pharmabill.app.data.PharmaDao) {
        val dialogBinding = DialogPickMedicineBinding.inflate(layoutInflater)
        val pickerAdapter = MedicineAdapter { medicine ->
            val current = cart.value
            val existing = current.firstOrNull { it.medicine.id == medicine.id }
            val newQty = (existing?.qty ?: 0) + 1
            if (newQty > medicine.stock) {
                Toast.makeText(
                    requireContext(), R.string.pick_over_stock,
                    Toast.LENGTH_SHORT
                ).show()
                return@MedicineAdapter
            }
            cart.value = if (existing == null) {
                current + CartLine(medicine, 1)
            } else {
                current.map {
                    if (it.medicine.id == medicine.id) it.copy(qty = newQty)
                    else it
                }
            }
        }
        dialogBinding.pickerList.layoutManager =
            LinearLayoutManager(requireContext())
        dialogBinding.pickerList.adapter = pickerAdapter
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.pick_medicine)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.done, null)
            .create()
        val query = MutableStateFlow("")
        dialogBinding.pickerSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                query.value = s?.toString()?.trim() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        viewLifecycleOwner.lifecycleScope.launch {
            query.collect { q ->
                val all = if (q.isEmpty()) dao.medicines().first()
                    else dao.searchMedicines(q).first()
                // Only sellable batches: active (DAO already filters) and
                // in stock. FEFO order comes from the DAO ordering.
                pickerAdapter.submit(all.filter { it.stock > 0 })
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
