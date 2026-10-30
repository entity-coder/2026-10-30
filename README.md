# PharmaBill Android

A pharmacy billing app for Android: medicine catalog with batch/expiry
tracking, fast GST billing with printable/shareable invoices, expiry and
low-stock alerts, stock in/out with an audit log, and day-wise sales
reports. Fully offline-first — everything lives in a local Room database
on the phone, so it works at a pharmacy counter with no internet at all.

This is the Android port of the PharmaBill Python app (2026-10-21), built
for pharmacies and medical stores that want the same billing desk on a
phone.

## Features

- **Dashboard** — today's sales total, bills today, expired-batch count,
  expiring-soon count (within 90 days), low-stock count, and one alert list
  covering expired batches, batches expiring within 30 days, batches
  expiring within 90 days, and low-stock items. Tapping an alert opens
  that medicine's detail screen.
- **Medicines** — catalog with name, batch, optional company, expiry date
  (yyyy-MM-dd), MRP, GST %, stock, and a low-stock threshold. Search by
  name or batch. Add and edit from a dialog; the detail screen also has
  stock in/out and the movement history.
- **Billing** — pick batches from a searchable picker that lists
  earliest-expiry first within each medicine name (FEFO: first-expiry,
  first-out), so old stock sells before it expires. Quantities are capped
  at available stock — the picker refuses to add beyond stock, the cart's
  plus button disables at the stock limit, and saving re-checks stock and
  refuses the whole bill if anything is now over-sold. A bill saves only
  with at least one line. Saving deducts stock and writes one stock-log
  row per line ("Sale PB-0001", …).
- **GST breakup** — MRP is GST-inclusive. Each bill shows subtotal,
  taxable value, CGST, and SGST (the GST split equally in halves; an odd
  paise goes to CGST so the halves always add up to the GST total).
- **Invoices** — every bill gets a sequential number (PB-0001, PB-0002,
  …) and a PDF invoice you can print via the system print dialog or
  share (FileProvider) from the bill detail screen. The shop name printed
  on the invoice is set in Shop settings from the Dashboard.
- **Stock in/out** — adjust stock with a reason (e.g. "New purchase",
  "Damaged"). Every adjustment — manual or from a sale — is recorded in
  an append-only stock log shown per medicine, newest first.
- **Delete guard** — a batch that appears on any bill cannot be deleted
  (that would break old invoices). The app offers deactivation instead:
  the batch stays on old bills but disappears from the catalog and the
  billing picker. Batches never billed can be deleted outright.
- **Reports** — pick a date (defaults to today) and see that day's bills
  with bill count, total amount, and GST total. Tapping a bill opens it
  for printing or sharing.
- **Offline-first** — Room database, no account, no server, no internet
  needed. Data stays on the device.

## Money math

All money is stored and computed as **integer paise** (`Long`) — MRP,
line totals, subtotals, GST, and totals. No `Double` is ever used for a
money total, so totals never accumulate floating-point error. The one
`Double` in the money path is inside `gstBreakup`, where the taxable
value is computed as `inclusive * 100 / (100 + rate)` and rounded to the
nearest paise with `roundToLong()` — a single rounding step per line,
not an accumulation. Display formatting (`fmtINR`) divides by 100.0 only
for rendering.

## How to open and run in Android Studio

This project was **not** compiled or tested on a device here (this machine
has no Android SDK). To run it:

1. Open Android Studio and choose **Open**, then select this folder.
2. Let Gradle sync (it downloads the Android Gradle Plugin 8.2.2, Kotlin
   1.9.22, and the Room/Material dependencies on first sync).
3. Connect a device or start an emulator, then press **Run**.
4. Requirements: Android Studio Hedgehog or newer, JDK 17, minSdk 24
   (Android 7.0+).

The kapt plugin version is declared explicitly and exactly once
(`1.9.22`, matching the Kotlin version) in `app/build.gradle.kts` — the
GitHub Actions APK build fails if kapt is applied without a version.

## Direct APK download

Once the GitHub Actions workflow (see below) is added to the repo and a
build succeeds, a debug APK is published to a rolling release, and the
direct download link will be:

`https://github.com/entity-coder/2026-10-30/releases/download/apk-latest/PharmaBill-v1.0.apk`

**That link does not work yet** — the release only exists after the
workflow file is added to the repo and the first build succeeds. Until
then, open the project in Android Studio as described above.

### One manual step for the APK build

The GitHub integration cannot write `.github/workflows/` files, so the
build workflow is handed over separately: add `.github/workflows/build-apk.yml`
to the repo once via the GitHub web UI (Add file → Create new file, with
the leading dot in `.github` — without it, GitHub Actions silently ignores
the file), then untick "Enable release immutability" in repo Settings so
the rolling release can update. After that, every push builds a fresh APK.

## Project structure

```
app/src/main/java/com/pharmabill/app/
  data/
    Medicine.kt        # Catalog entity (one row per batch)
    Bill.kt            # Bill + BillLine entities
    StockMovement.kt   # Append-only stock log entity
    PharmaDao.kt       # Room DAO (queries, FEFO ordering, delete guard)
    PharmaDatabase.kt  # Room singleton (pharmabill.db)
    MoneyMath.kt       # Pure billing maths: GST breakup, bill numbers,
                       # expiry buckets, rupees↔paise parsing
    Format.kt          # Date + ₹ formatting
    ShopPrefs.kt       # Shop name in SharedPreferences
  ui/
    MainActivity.kt        # Bottom-nav host (4 tabs)
    DashboardFragment.kt   # Today's sales + expiry/low-stock alerts
    AlertAdapter.kt
    MedicinesFragment.kt   # Catalog + search
    MedicineDialog.kt      # Shared add/edit dialog
    MedicineDetailActivity.kt  # Detail, stock in/out, movements, delete guard
    MovementAdapter.kt
    BillingFragment.kt     # Cart, FEFO picker, over-sell guards, save
    BillingLineAdapter.kt
    ReportsFragment.kt     # Day-wise sales report
    BillAdapter.kt
    BillDetailActivity.kt  # Saved bill + print/share invoice
    BillLineViewAdapter.kt
    InvoicePdf.kt          # PdfDocument invoice + PrintManager printing
    SettingsActivity.kt    # Shop name setting
```

## Interview notes

**What it is.** PharmaBill Android is an offline-first pharmacy billing
app: a medicine catalog (one row per batch, with expiry/MRP/GST/stock),
fast GST billing with FEFO batch selection, printable/shareable PDF
invoices, expiry and low-stock alerts, an append-only stock log, and
day-wise sales reports — all in a local Room database.

**Why Room + Flow + repeatOnLifecycle.** Room gives a typed, queryable
local store with compile-time-checked SQL, which matters for a billing
app where a wrong query corrupts money or stock. DAOs expose `Flow`, and
screens collect with `repeatOnLifecycle(STARTED)`, so lists refresh
automatically when stock changes or a bill saves, and collection stops
when the screen is off — no manual refresh calls, no leaks from
collecting in a destroyed view.

**Why integer paise, not Double.** Money in binary floating point
accumulates rounding error (0.1 + 0.2 ≠ 0.3). Storing paise as `Long`
makes every total exact. The only rounding is a single
`roundToLong()` per line when extracting the GST-exclusive taxable value
from a GST-inclusive MRP; the CGST/SGST split then divides that GST
exactly, with an odd paise going to CGST so the halves always sum to the
total. Trade-off: amounts are capped by `Long` range (irrelevant at
pharmacy scale) and paise must be converted for display, which `fmtINR`
does in one place.

**Why one catalog row per batch.** Expiry, MRP, and stock are properties
of a batch, not of a medicine name. Separate rows let the billing picker
order by expiry within a name and sell the earliest-expiring batch first
(FEFO), which is the single most valuable behaviour for a pharmacy —
it directly reduces expired-stock losses. The cost is a slightly larger
catalog and the need for the delete guard below.

**Why deactivate instead of deleting billed batches.** Bill lines copy
the medicine name and batch at sale time, but they still reference the
batch by id for the guard check. Deleting a billed batch would orphan
history and could make old invoices unexplainable, so the app refuses
deletion when any bill line references the batch and offers deactivation
(`active = false`) instead: the batch vanishes from the catalog and the
picker but stays on old bills. Unbilled batches can be deleted outright.

**Why the over-sell check happens twice.** The picker and cart stepper
cap quantities at the stock shown on screen, but stock can change
between picking and saving (in principle, another screen or a second
save). So saving re-reads each batch and refuses the entire bill if any
line exceeds current stock — a bill is all-or-nothing, never partially
deducted. Trade-off: the check and the deduction are not in a single
Room transaction, so a crash between them could theoretically leave a
bill saved without deduction; wrapping the save in `@Transaction` is the
first extension.

**Why the stock log is append-only.** Sales and adjustments both write
rows that are never edited or deleted, so a medicine's movement history
is an audit trail a shop owner can trust when stock numbers don't add
up. The cost is unbounded growth, but at pharmacy volume a few thousand
rows a year is negligible.

**Why the shop name lives in SharedPreferences, not Room.** It is a
single scalar setting, not relational data — a Room table for it would
be ceremony without benefit. If settings grow (address, phone, GSTIN on
invoices), they should move to a proper settings store.

**Trade-offs / limitations.**

- No barcode scanning, no multi-user sync, no cloud backup — the database
  lives on one device. A shop with two counters needs an export/sync
  story that doesn't exist yet.
- No GSTIN, no HSN codes, no discount or return handling, and no
  customer ledger — invoices show shop name, bill number, date, lines,
  and the GST breakup only.
- Expiry dates are typed as yyyy-MM-dd text and validated by regex only;
  a date like 2026-02-30 passes the regex and sorts oddly. A date picker
  would fix this.
- Bill saving is not wrapped in a single Room `@Transaction` (see above).
- The app was not compiled or tested on a device here (no Android SDK
  on this machine); it is expected to open and run in Android Studio as
  described above, and the APK link works only after the workflow is
  added and a build succeeds.

**How I'd extend it.** Wrap bill saving in a `@Transaction`; add a date
picker for expiry/report dates; add barcode scanning for the billing
picker; add discount per line and sales returns (negative-quantity bills
with credit notes); add GSTIN/HSN fields and a proper invoice header;
add backup/restore (export the Room database file) and, later, a
multi-device sync.
