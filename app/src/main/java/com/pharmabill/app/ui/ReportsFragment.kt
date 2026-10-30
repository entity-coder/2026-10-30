package com.pharmabill.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.pharmabill.app.data.PharmaDatabase
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.data.todayIso
import com.pharmabill.app.databinding.FragmentReportsBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Day-wise sales report: pick a date (defaults to today), see that day's
 * bills with count, total amount, and total GST. Tapping a bill opens it.
 */
class ReportsFragment : Fragment() {
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val date = MutableStateFlow(todayIso())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dao = PharmaDatabase.get(requireContext()).dao()
        val adapter = BillAdapter { bill ->
            startActivity(
                Intent(requireContext(), BillDetailActivity::class.java)
                    .putExtra(BillDetailActivity.EXTRA_BILL_ID, bill.id)
            )
        }
        binding.billList.layoutManager = LinearLayoutManager(requireContext())
        binding.billList.adapter = adapter
        binding.dateInput.setText(todayIso())
        binding.showButton.setOnClickListener {
            val input = binding.dateInput.text.toString().trim()
            if (input.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                date.value = input
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                date.flatMapLatest { dao.billsForDate(it) }.collect { bills ->
                    adapter.submit(bills)
                    binding.summaryText.text =
                        "${bills.size} bill(s) · " +
                        "Total ${fmtINR(bills.sumOf { it.totalPaise })} · " +
                        "GST ${fmtINR(bills.sumOf { it.gstPaise })}"
                    binding.noBills.visibility =
                        if (bills.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
