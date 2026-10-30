package com.pharmabill.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pharmabill.app.data.Bill
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.databinding.ItemBillBinding

class BillAdapter(private val onClick: (Bill) -> Unit) :
    RecyclerView.Adapter<BillAdapter.ViewHolder>() {

    private var items: List<Bill> = emptyList()

    fun submit(newItems: List<Bill>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemBillBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBillBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bill = items[position]
        holder.binding.billNo.text = bill.billNo
        holder.binding.billDetail.text =
            (bill.customerName?.let { "$it · " } ?: "") +
            "Total ${fmtINR(bill.totalPaise)}"
        holder.binding.root.setOnClickListener { onClick(bill) }
    }

    override fun getItemCount(): Int = items.size
}
