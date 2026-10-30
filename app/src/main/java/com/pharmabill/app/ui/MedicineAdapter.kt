package com.pharmabill.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pharmabill.app.data.Medicine
import com.pharmabill.app.data.fmtGst
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.databinding.ItemMedicineBinding

class MedicineAdapter(private val onClick: (Medicine) -> Unit) :
    RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

    private var items: List<Medicine> = emptyList()

    fun submit(newItems: List<Medicine>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemMedicineBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medicine = items[position]
        holder.binding.medicineName.text = medicine.name
        holder.binding.medicineMeta.text =
            "Batch ${medicine.batch} · Exp ${medicine.expiry} · " +
            "GST ${fmtGst(medicine.gstPercent)}" +
            (medicine.company?.let { " · $it" } ?: "")
        holder.binding.medicineStock.text =
            "Stock: ${medicine.stock} · ${fmtINR(medicine.mrpPaise)}"
        holder.binding.root.setOnClickListener { onClick(medicine) }
    }

    override fun getItemCount(): Int = items.size
}
