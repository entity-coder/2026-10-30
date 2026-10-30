package com.pharmabill.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pharmabill.app.data.StockMovement
import com.pharmabill.app.databinding.ItemMovementBinding

/** Read-only rows from the append-only stock log, newest first. */
class MovementAdapter : RecyclerView.Adapter<MovementAdapter.ViewHolder>() {

    private var items: List<StockMovement> = emptyList()

    fun submit(newItems: List<StockMovement>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemMovementBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movement = items[position]
        val sign = if (movement.delta >= 0) "+" else ""
        holder.binding.movementLine.text =
            "$sign${movement.delta} · ${movement.reason}"
        holder.binding.movementDate.text = movement.date
    }

    override fun getItemCount(): Int = items.size
}
