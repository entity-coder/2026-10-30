package com.pharmabill.app.data

import android.content.Context

/** Shop name shown on invoices, kept in SharedPreferences (a single
 *  setting does not justify a Room table). */
object ShopPrefs {
    private const val FILE = "pharmabill_prefs"
    private const val KEY_SHOP = "shop_name"
    const val DEFAULT_SHOP = "PharmaBill Pharmacy"

    fun shopName(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_SHOP, DEFAULT_SHOP) ?: DEFAULT_SHOP

    fun setShopName(context: Context, name: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_SHOP, name).apply()
    }
}
