package com.pharmabill.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pharmabill.app.data.BillLine
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.databinding.ItemBillViewBinding

/** Read-only saved-bill lines for the detail screen and reports. */
class BillLineViewAdapter : RecyclerView.Adapter<BillLineViewAdapter.ViewHolder>() {

    private var items: List<BillLine> = emptyList()

    fun submit(newItems: List<BillLine>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemBillViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBillViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = items[position]
        holder.binding.billLineName.text =
            "${line.medicineName} — batch ${line.batch}"
        holder.binding.billLineDetail.text =
            "${line.qty} x ${fmtINR(line.mrpPaise)} = ${fmtINR(line.lineTotalPaise)}"
    }

    override fun getItemCount(): Int = items.size
}
