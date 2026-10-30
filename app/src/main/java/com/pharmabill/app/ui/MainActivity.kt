package com.pharmabill.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.pharmabill.app.R
import com.pharmabill.app.data.PharmaDatabase
import com.pharmabill.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Warm the Room singleton on launch so later screens never block on it.
        PharmaDatabase.get(this)
        if (savedInstanceState == null) show(DashboardFragment())
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_medicines -> MedicinesFragment()
                R.id.nav_billing -> BillingFragment()
                R.id.nav_reports -> ReportsFragment()
                else -> return@setOnItemSelectedListener false
            }
            show(fragment)
            true
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
