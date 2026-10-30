package com.pharmabill.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pharmabill.app.data.Medicine
import com.pharmabill.app.databinding.ItemAlertBinding

/** One alert row: the medicine plus a human-readable reason. */
data class AlertRow(val medicine: Medicine, val reason: String)

class AlertAdapter(private val onClick: (Medicine) -> Unit) :
    RecyclerView.Adapter<AlertAdapter.ViewHolder>() {

    private var rows: List<AlertRow> = emptyList()

    fun submit(newRows: List<AlertRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemAlertBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        holder.binding.alertTitle.text =
            "${row.medicine.name} — batch ${row.medicine.batch}"
        holder.binding.alertReason.text = row.reason
        holder.binding.root.setOnClickListener { onClick(row.medicine) }
    }

    override fun getItemCount(): Int = rows.size
}
