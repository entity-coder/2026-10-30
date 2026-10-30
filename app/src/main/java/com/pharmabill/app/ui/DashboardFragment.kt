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
import com.pharmabill.app.data.ExpiryBucket
import com.pharmabill.app.data.PharmaDatabase
import com.pharmabill.app.data.expiryBucket
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.data.todayIso
import com.pharmabill.app.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch

/**
 * At-a-glance numbers plus the three alert lists: expired batches,
 * batches expiring within 30 days, within 90 days, and low-stock items.
 * Tapping an alert row opens that medicine's detail screen.
 */
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dao = PharmaDatabase.get(requireContext()).dao()
        val alertAdapter = AlertAdapter { medicine ->
            startActivity(
                Intent(requireContext(), MedicineDetailActivity::class.java)
                    .putExtra(MedicineDetailActivity.EXTRA_MEDICINE_ID, medicine.id)
            )
        }
        binding.alertList.layoutManager = LinearLayoutManager(requireContext())
        binding.alertList.adapter = alertAdapter
        val today = todayIso()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                kotlinx.coroutines.flow.combine(
                    dao.medicines(), dao.billsForDate(today)
                ) { medicines, todayBills ->
                    val expired = medicines.filter {
                        expiryBucket(it.expiry, today) == ExpiryBucket.EXPIRED
                    }
                    val within30 = medicines.filter {
                        expiryBucket(it.expiry, today) == ExpiryBucket.WITHIN_30
                    }
                    val within90 = medicines.filter {
                        expiryBucket(it.expiry, today) == ExpiryBucket.WITHIN_90
                    }
                    val lowStock = medicines.filter { it.stock <= it.lowStockThreshold }
                    DashboardData(
                        todaySales = todayBills.sumOf { it.totalPaise },
                        billsToday = todayBills.size,
                        expired = expired,
                        within30 = within30,
                        within90 = within90,
                        lowStock = lowStock
                    )
                }.collect { data ->
                    binding.todaySales.text = fmtINR(data.todaySales)
                    binding.billsToday.text = data.billsToday.toString()
                    binding.expiredCount.text = data.expired.size.toString()
                    binding.expiringSoonCount.text =
                        (data.within30.size + data.within90.size).toString()
                    binding.lowStockCount.text = data.lowStock.size.toString()
                    val alerts = mutableListOf<AlertRow>()
                    data.expired.forEach {
                        alerts += AlertRow(it, "Expired on ${it.expiry}")
                    }
                    data.within30.forEach {
                        alerts += AlertRow(it, "Expires ${it.expiry} (within 30 days)")
                    }
                    data.within90.forEach {
                        alerts += AlertRow(it, "Expires ${it.expiry} (within 90 days)")
                    }
                    data.lowStock.forEach {
                        alerts += AlertRow(it, "Low stock: ${it.stock} left (threshold ${it.lowStockThreshold})")
                    }
                    alertAdapter.submit(alerts)
                    binding.noAlerts.visibility =
                        if (alerts.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class DashboardData(
        val todaySales: Long,
        val billsToday: Int,
        val expired: List<com.pharmabill.app.data.Medicine>,
        val within30: List<com.pharmabill.app.data.Medicine>,
        val within90: List<com.pharmabill.app.data.Medicine>,
        val lowStock: List<com.pharmabill.app.data.Medicine>
    )
}
