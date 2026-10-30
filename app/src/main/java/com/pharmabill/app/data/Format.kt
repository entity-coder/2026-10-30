package com.pharmabill.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Today's date as yyyy-MM-dd in the device's default timezone. */
fun todayIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/** Format integer paise as ₹ with two decimals. */
fun fmtINR(paise: Long): String =
    "₹%,.2f".format(Locale.ENGLISH, paise / 100.0)

/** GST percent as a short label, e.g. "12%". */
fun fmtGst(percent: Int): String = "$percent%"
