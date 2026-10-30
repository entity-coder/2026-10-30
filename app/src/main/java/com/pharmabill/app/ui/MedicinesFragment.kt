package com.pharmabill.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.pharmabill.app.data.PharmaDatabase
import com.pharmabill.app.databinding.FragmentMedicinesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Medicine catalog: search by name/batch, add, and tap a row to edit,
 * adjust stock, or see its movement history.
 */
class MedicinesFragment : Fragment() {
    private var _binding: FragmentMedicinesBinding? = null
    private val binding get() = _binding!!
    private val query = MutableStateFlow("")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dao = PharmaDatabase.get(requireContext()).dao()
        val adapter = MedicineAdapter { medicine ->
            startActivity(
                Intent(requireContext(), MedicineDetailActivity::class.java)
                    .putExtra(MedicineDetailActivity.EXTRA_MEDICINE_ID, medicine.id)
            )
        }
        binding.medicineList.layoutManager = LinearLayoutManager(requireContext())
        binding.medicineList.adapter = adapter
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                query.value = s?.toString()?.trim() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                query.flatMapLatest { q ->
                    if (q.isEmpty()) dao.medicines() else dao.searchMedicines(q)
                }.collect { adapter.submit(it) }
            }
        }
        binding.addButton.setOnClickListener {
            showMedicineDialog(
                requireContext(), viewLifecycleOwner.lifecycleScope, dao, null
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
