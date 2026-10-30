package com.pharmabill.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.pharmabill.app.R
import com.pharmabill.app.data.Bill
import com.pharmabill.app.data.PharmaDatabase
import com.pharmabill.app.data.ShopPrefs
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.databinding.ActivityBillDetailBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/** Saved bill view: lines, totals, and Print / Share invoice actions. */
class BillDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_BILL_ID = "bill_id"
    }

    private lateinit var binding: ActivityBillDetailBinding
    private var bill: Bill? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dao = PharmaDatabase.get(this).dao()
        val billId = intent.getLongExtra(EXTRA_BILL_ID, -1)
        val lineAdapter = BillLineViewAdapter()
        binding.billLinesList.layoutManager = LinearLayoutManager(this)
        binding.billLinesList.adapter = lineAdapter
        lifecycleScope.launch {
            val loaded = dao.billById(billId)
            if (loaded == null) {
                Toast.makeText(this@BillDetailActivity,
                    R.string.bill_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            bill = loaded
            binding.billHeader.text =
                "${loaded.billNo} · ${loaded.date}" +
                (loaded.customerName?.let { " · $it" } ?: "")
            binding.billTotals.text =
                "Subtotal ${fmtINR(loaded.subtotalPaise)} · " +
                "GST ${fmtINR(loaded.gstPaise)} · " +
                "Total ${fmtINR(loaded.totalPaise)}"
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.linesForBill(billId).collect { lineAdapter.submit(it) }
            }
        }
        binding.printButton.setOnClickListener {
            lifecycleScope.launch {
                val file = buildInvoice() ?: return@launch
                InvoicePdf.print(this@BillDetailActivity, file, bill?.billNo ?: "Invoice")
            }
        }
        binding.shareButton.setOnClickListener {
            lifecycleScope.launch {
                val file = buildInvoice() ?: return@launch
                val uri = FileProvider.getUriForFile(
                    this@BillDetailActivity,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(
                    Intent.createChooser(intent, getString(R.string.share_invoice))
                )
            }
        }
    }

    /** Generate the invoice PDF fresh from the saved bill. Lines are
     *  immutable after saving, so the PDF always matches the bill. */
    private suspend fun buildInvoice(): File? {
        val current = bill ?: return null
        val dao = PharmaDatabase.get(this).dao()
        val lines = dao.linesForBill(current.id).first()
        val file = InvoicePdf.create(
            this, ShopPrefs.shopName(this), current, lines
        )
        if (file == null) {
            Toast.makeText(this, R.string.invoice_failed, Toast.LENGTH_SHORT).show()
        }
        return file
    }
}
