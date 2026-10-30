package com.pharmabill.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.data.lineTotalPaise
import com.pharmabill.app.databinding.ItemBillLineBinding

/**
 * Cart rows: name/batch, quantity stepper (capped at stock — the plus
 * button disables at the stock limit), line total, remove button.
 */
class BillingLineAdapter(
    private val onQtyChange: (CartLine, Int) -> Unit,
    private val onRemove: (CartLine) -> Unit
) : RecyclerView.Adapter<BillingLineAdapter.ViewHolder>() {

    private var items: List<CartLine> = emptyList()

    fun submit(newItems: List<CartLine>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemBillLineBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBillLineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = items[position]
        val med = line.medicine
        holder.binding.lineName.text = "${med.name} — batch ${med.batch}"
        holder.binding.lineQty.text = line.qty.toString()
        holder.binding.lineTotal.text =
            fmtINR(lineTotalPaise(med.mrpPaise, line.qty))
        holder.binding.minusButton.isEnabled = line.qty > 1
        holder.binding.plusButton.isEnabled = line.qty < med.stock
        holder.binding.minusButton.setOnClickListener {
            onQtyChange(line, line.qty - 1)
        }
        holder.binding.plusButton.setOnClickListener {
            onQtyChange(line, line.qty + 1)
        }
        holder.binding.removeButton.setOnClickListener { onRemove(line) }
    }

    override fun getItemCount(): Int = items.size
}
