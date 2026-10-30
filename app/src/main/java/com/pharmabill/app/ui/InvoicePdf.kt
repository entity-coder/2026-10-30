package com.pharmabill.app.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.pharmabill.app.data.Bill
import com.pharmabill.app.data.BillLine
import com.pharmabill.app.data.fmtGst
import com.pharmabill.app.data.fmtINR
import com.pharmabill.app.data.gstBreakup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * One-page-ish invoice PDF (A5-ish page, grows to extra pages if the bill
 * has many lines), written to the app cache for sharing/printing.
 * GST is shown as an equal CGST/SGST split of the embedded GST.
 */
object InvoicePdf {
    fun create(
        context: Context, shopName: String, bill: Bill, lines: List<BillLine>
    ): File? {
        return try {
            val document = PdfDocument()
            val title = Paint().apply { textSize = 26f; isFakeBoldText = true }
            val body = Paint().apply { textSize = 15f }
            val small = Paint().apply { textSize = 13f }
            var pageNo = 1
            var page = document.startPage(
                PdfDocument.PageInfo.Builder(595, 842, pageNo).create()
            )
            var y = 80f
            fun newPageIfNeeded() {
                if (y > 780f) {
                    document.finishPage(page)
                    pageNo++
                    page = document.startPage(
                        PdfDocument.PageInfo.Builder(595, 842, pageNo).create()
                    )
                    y = 60f
                }
            }
            fun line(text: String, paint: Paint = body, gap: Float = 30f) {
                newPageIfNeeded()
                page.canvas.drawText(text, 40f, y, paint)
                y += gap
            }
            line(shopName, title, 44f)
            line("Invoice: ${bill.billNo}    Date: ${bill.date}")
            bill.customerName?.let { line("Customer: $it") }
            y += 10f
            lines.forEach { item ->
                line(
                    "${item.medicineName} (${item.batch}) x ${item.qty} @ " +
                    "${fmtINR(item.mrpPaise)} = ${fmtINR(item.lineTotalPaise)}",
                    small, 24f
                )
            }
            y += 14f
            line("Subtotal: ${fmtINR(bill.subtotalPaise)}")
            // Taxable base + CGST/SGST recomputed from the stored lines so
            // the printed breakup always matches the saved bill.
            var taxable = 0L
            var cgst = 0L
            var sgst = 0L
            lines.forEach {
                val breakup = gstBreakup(it.lineTotalPaise, it.gstPercent)
                taxable += breakup.taxablePaise
                cgst += breakup.cgstPaise
                sgst += breakup.sgstPaise
            }
            line("Taxable value: ${fmtINR(taxable)}")
            line("CGST: ${fmtINR(cgst)}    SGST: ${fmtINR(sgst)}")
            line("GST total: ${fmtINR(bill.gstPaise)}")
            line("Total: ${fmtINR(bill.totalPaise)}", title, 44f)
            y += 10f
            line("Thank you for shopping with us.", small)
            document.finishPage(page)
            val file = File(context.cacheDir, "${bill.billNo}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    /** Print the already-generated PDF file via the system print dialog. */
    fun print(context: Context, file: File, jobName: String) {
        val printManager =
            context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(file.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileInputStream(file).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { out ->
                                input.copyTo(out)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            },
            null
        )
    }
}
