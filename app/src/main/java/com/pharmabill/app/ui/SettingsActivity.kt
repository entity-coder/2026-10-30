package com.pharmabill.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pharmabill.app.R
import com.pharmabill.app.data.ShopPrefs
import com.pharmabill.app.databinding.ActivitySettingsBinding

/** Shop settings: the shop name printed on every invoice. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.shopNameInput.setText(ShopPrefs.shopName(this))
        binding.saveSettingsButton.setOnClickListener {
            val name = binding.shopNameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.shop_name_empty,
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ShopPrefs.setShopName(this, name)
            Toast.makeText(this, R.string.settings_saved,
                Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
