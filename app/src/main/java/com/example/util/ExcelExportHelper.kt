package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.AccountWithBalance
import com.example.data.model.TopPurchaserItem
import com.example.data.model.OverdueAccountItem
import com.example.data.model.OverdueSeverity
import com.example.ui.components.formatMoney
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    /**
     * Builds a comprehensive WhatsApp statement summary with all transactions and current net balance (جرد كامل).
     * Note: No external links are attached, purely text summary.
     */
    fun createStatementSummaryWhatsAppMessage(
        account: AccountEntity,
        transactions: List<TransactionEntity>,
        netBalance: Double
    ): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val exportDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date())
        val displayNum = if (account.displayIndex > 0) account.displayIndex else account.id

        val lanaTotal = transactions.filter { it.type == "LANA" }.sumOf { it.amount }
        val lahoTotal = transactions.filter { it.type == "LAHO" }.sumOf { it.amount }

        return buildString {
            appendLine("📄 *كشف حساب مالي وجرد كامل - تطبيق البيان*")
            appendLine("👤 *العميل:* ${account.name}")
            appendLine("🔖 *رقم الحساب:* #$displayNum")
            appendLine("📅 *تاريخ الجرد:* $exportDate")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📊 *ملخص الرصيد المالي الحالي:*")
            if (netBalance > 0) {
                appendLine("👉 *المبلغ المطلوب سداده (لنا):* ${formatMoney(netBalance)} ${account.currency} 🔴")
            } else if (netBalance < 0) {
                appendLine("👉 *رصيد دائن لكم بذمتنا (له):* ${formatMoney(-netBalance)} ${account.currency} 🟢")
            } else {
                appendLine("👉 *الحساب خالص ومطابق تماماً (0 ${account.currency})* ⚪")
            }
            appendLine("• إجمالي المسحوبات (لنا): ${formatMoney(lanaTotal)} ${account.currency}")
            appendLine("• إجمالي المدفوعات (له): ${formatMoney(lahoTotal)} ${account.currency}")
            appendLine("• إجمالي عدد العمليات: ${transactions.size}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📋 *سجل وجرد العمليات بالكامل:*")
            val sortedTx = transactions.sortedBy { it.date }
            if (sortedTx.isEmpty()) {
                appendLine("- لا توجد عمليات سابقة.")
            } else {
                sortedTx.forEachIndexed { index, tx ->
                    val typeLabel = if (tx.type == "LANA") "لنا (+)" else "له (-)"
                    val dateStr = sdf.format(Date(tx.date))
                    val descStr = if (tx.description.isNotBlank()) " | ${tx.description.trim()}" else ""
                    val receiptStr = if (tx.receiptNumber.isNotBlank()) " [سند #${tx.receiptNumber.trim()}]" else ""
                    appendLine("${index + 1}. $dateStr : ${formatMoney(tx.amount)} ${tx.currency} ($typeLabel)$descStr$receiptStr")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✨ تم التوثيق وتحديث الرصيد عبر تطبيق البيان.")
        }
    }

    /**
     * Builds and sends the WhatsApp Invoice message immediately after saving an invoice/transaction
     * with complete inventory (جرد كامل للحساب مع الفاتورة الجديدة والرصيد السابق والجديد وتفاصيل العمليات).
     */
    fun createInvoiceWhatsAppMessage(
        account: AccountEntity,
        type: String, // "LANA" or "LAHO"
        invoiceAmount: Double,
        oldBalance: Double = 0.0,
        newBalance: Double = 0.0,
        description: String,
        dateMillis: Long = System.currentTimeMillis(),
        receiptNumber: String = "",
        transactions: List<TransactionEntity> = emptyList()
    ): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault())
        val dateFormatted = sdf.format(Date(dateMillis))

        val isLana = type == "LANA"
        val operationTitle = if (isLana) "فاتورة مشتريات / دين لنا (+)" else "سداد دفعة نقدية / دفعة له (-)"
        val descText = if (description.isNotBlank()) description.trim() else if (isLana) "فاتورة بضاعة" else "سند قبض نقدي"

        val oldBalText = if (oldBalance > 0) "لنا ${formatMoney(oldBalance)}" else if (oldBalance < 0) "له ${formatMoney(-oldBalance)}" else "0 خالص"
        val newBalText = if (newBalance > 0) "المطلوب سداده (لنا): ${formatMoney(newBalance)} ${account.currency} 🔴" else if (newBalance < 0) "رصيد دائن لكم (له): ${formatMoney(-newBalance)} ${account.currency} 🟢" else "الحساب خالص ومطابق تماماً (0 ${account.currency}) ⚪"

        return buildString {
            appendLine("🧾 *إشعار فاتورة ومعاملة جديدة - تطبيق البيان*")
            appendLine("👤 *العميل / الحساب:* ${account.name}")
            appendLine("📅 *تاريخ الفاتورة:* $dateFormatted")
            if (receiptNumber.isNotBlank()) {
                appendLine("🔢 *رقم الفاتورة/السند:* $receiptNumber")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📄 *تفاصيل الحركة الجديدة:*")
            appendLine("• نوع العملية: $operationTitle")
            appendLine("• المبلغ: ${formatMoney(invoiceAmount)} ${account.currency}")
            appendLine("• البيان: $descText")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📊 *الموقف المالي والرصيد الصافي:*")
            appendLine("• الرصيد السابق: $oldBalText ${account.currency}")
            appendLine("• الحركة الحالية: ${if (isLana) "+" else "-"}${formatMoney(invoiceAmount)} ${account.currency}")
            appendLine("👉 *الرصيد الصافي الجديد:* $newBalText")

            // جرد كامل لجميع العمليات والحركات السابقة والحالية
            if (transactions.isNotEmpty()) {
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📋 *جرد العمليات والحركات بالكامل:*")
                val dateOnlySdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                val sortedTx = transactions.sortedBy { it.date }
                sortedTx.forEachIndexed { index, tx ->
                    val typeLabel = if (tx.type == "LANA") "لنا (+)" else "له (-)"
                    val dateOnly = dateOnlySdf.format(Date(tx.date))
                    val desc = if (tx.description.isNotBlank()) " | ${tx.description.trim()}" else ""
                    val rec = if (tx.receiptNumber.isNotBlank()) " [سند #${tx.receiptNumber.trim()}]" else ""
                    appendLine("${index + 1}. $dateOnly : ${formatMoney(tx.amount)} ${tx.currency} ($typeLabel)$desc$rec")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✨ تم التوثيق وتحديث الرصيد آلياً عبر تطبيق البيان.")
        }
    }

    /**
     * Direct redirect to WhatsApp on the person's phone number.
     */
    fun sendWhatsAppInvoice(context: Context, phone: String, message: String) {
        try {
            val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "").trim()
            val uri = if (cleanPhone.isNotBlank()) {
                Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
            } else {
                Uri.parse("whatsapp://send?text=${Uri.encode(message)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to share intent if WhatsApp direct deep-link fails or app missing
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "إرسال الفاتورة عبر واتساب"))
        }
    }

    /**
     * Exports a single account's statement to a clean, universal Excel spreadsheet (.xls)
     * with full styling, colors, right-to-left layout, and clear Net Balance prominence.
     */
    fun exportAccountToExcel(
        context: Context,
        account: AccountEntity,
        transactions: List<TransactionEntity>,
        storeName: String = "البيان"
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanName = account.name.replace(Regex("[^a-zA-Z0-9ء-ي_]"), "_")
            val fileName = "كشف_حساب_${cleanName}_البيان.xls"
            val file = File(exportDir, fileName)

            val sortedTx = transactions.sortedBy { it.date }
            val totalLana = sortedTx.filter { it.type == "LANA" }.sumOf { it.amount }
            val totalLaho = sortedTx.filter { it.type == "LAHO" }.sumOf { it.amount }
            val netBalance = totalLana - totalLaho

            val htmlContent = generateSingleAccountExcelHtml(
                account = account,
                transactions = sortedTx,
                totalLana = totalLana,
                totalLaho = totalLaho,
                netBalance = netBalance,
                storeName = storeName
            )

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\uFEFF") // UTF-8 BOM for Microsoft Excel
                    writer.write(htmlContent)
                }
            }

            shareExcelFile(context, file, "كشف حساب ${account.name} (Excel)")
            Toast.makeText(context, "تم تجهيز كشف حساب Excel الملون بنجاح ✓", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر تصدير ملف Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports all accounts to a comprehensive, beautifully styled, colored Excel spreadsheet (.xls)
     * with Account Name, Net Balance prominently highlighted, and KPI Summary Cards.
     */
    fun exportAllAccountsToExcel(
        context: Context,
        accounts: List<AccountWithBalance>,
        storeName: String = "البيان"
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "كشف_الحسابات_الشامل_البيان.xls"
            val file = File(exportDir, fileName)

            val htmlContent = generateAllAccountsExcelHtml(
                accounts = accounts,
                storeName = storeName
            )

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\uFEFF") // UTF-8 BOM for Microsoft Excel
                    writer.write(htmlContent)
                }
            }

            shareExcelFile(context, file, "كشف حسابات البيان الشامل (Excel)")
            Toast.makeText(context, "تم تجهيز كشف الحسابات الملون (.xls) بنجاح ✓", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر تصدير كشف Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Plain CSV export fallback for all accounts.
     */
    fun exportAllAccountsToCsv(
        context: Context,
        accounts: List<AccountWithBalance>
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "دليل_الحسابات_الشامل_البيان.csv"
            val file = File(exportDir, fileName)

            val exportDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date())
            val totalLana = accounts.sumOf { it.totalLana }
            val totalLaho = accounts.sumOf { it.totalLaho }
            val overallNet = totalLana - totalLaho

            val netOverallLabel = when {
                overallNet > 0 -> "إجمالي مطلوب لنا: ${formatMoney(overallNet)}"
                overallNet < 0 -> "إجمالي مطلوب علينا (له): ${formatMoney(-overallNet)}"
                else -> "الحسابات متوازنة تماماً (0.0)"
            }

            val csvContent = buildString {
                appendLine("تقرير دليل الحسابات المالي الشامل - تطبيق البيان")
                appendLine("تاريخ التصدير,${escapeCsv(exportDate)},إجمالي عدد الحسابات,${accounts.size}")
                appendLine("إجمالي ديون لنا (+),${formatMoney(totalLana)},إجمالي ديون علينا (-),${formatMoney(totalLaho)},صافي الرصيد العام,${escapeCsv(netOverallLabel)}")
                appendLine()
                appendLine("م,اسم الحساب / العميل,التصنيف,رقم الهاتف,العملة,إجمالي لنا (+),إجمالي علينا (-),المبلغ الصافي,حالة الحساب")
                accounts.forEachIndexed { index, item ->
                    val num = index + 1
                    val name = escapeCsv(item.account.name)
                    val cat = escapeCsv(item.account.category)
                    val phone = escapeCsv(item.account.phone.ifBlank { "-" })
                    val cur = escapeCsv(item.account.currency)
                    val lana = formatMoney(item.totalLana)
                    val laho = formatMoney(item.totalLaho)
                    val net = formatMoney(kotlin.math.abs(item.netBalance))
                    val statusText = when {
                        item.netBalance > 0 -> "لنا (+)"
                        item.netBalance < 0 -> "له (-)"
                        else -> "خالص"
                    }
                    appendLine("$num,$name,$cat,$phone,$cur,$lana,$laho,\"$net $cur\",$statusText")
                }
                appendLine()
                appendLine("المجموع الإجمالي,,,,,${formatMoney(totalLana)},${formatMoney(totalLaho)},$netOverallLabel,")
            }

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write(csvContent)
                }
            }

            shareExcelFile(context, file, "تقرير الحسابات (CSV)")
            Toast.makeText(context, "تم تجهيز ملف CSV بنجاح ✓", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر تصدير ملف CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateAllAccountsExcelHtml(
        accounts: List<AccountWithBalance>,
        storeName: String
    ): String {
        val exportDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date())
        val totalLana = accounts.sumOf { it.totalLana }
        val totalLaho = accounts.sumOf { it.totalLaho }
        val overallNet = totalLana - totalLaho

        val lanaCount = accounts.count { it.netBalance > 0 }
        val lahoCount = accounts.count { it.netBalance < 0 }
        val zeroCount = accounts.count { it.netBalance == 0.0 }

        val netBadgeText = when {
            overallNet > 0 -> "صافي مطلوب لنا (ديون مستحقة القبض)"
            overallNet < 0 -> "صافي مطلوب علينا (التزامات دائنة)"
            else -> "الحسابات متطابقة ومتوازنة تماماً"
        }

        val netBadgeColor = when {
            overallNet > 0 -> "#991B1B"
            overallNet < 0 -> "#166534"
            else -> "#334155"
        }

        val netBadgeBg = when {
            overallNet > 0 -> "#FEE2E2"
            overallNet < 0 -> "#DCFCE7"
            else -> "#F1F5F9"
        }

        return buildString {
            appendLine("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office"
                      xmlns:x="urn:schemas-microsoft-com:office:excel"
                      xmlns="http://www.w3.org/TR/REC-html40">
                <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                <!--[if gte mso 9]>
                <xml>
                 <x:ExcelWorkbook>
                  <x:ExcelWorksheets>
                   <x:ExcelWorksheet>
                    <x:Name>كشف الحسابات والأرصدة</x:Name>
                    <x:WorksheetOptions>
                     <x:DisplayRightToLeft/>
                     <x:DoNotDisplayGridlines/>
                    </x:WorksheetOptions>
                   </x:ExcelWorksheet>
                  </x:ExcelWorksheets>
                 </x:ExcelWorkbook>
                </xml>
                <![endif]-->
                <style>
                  body {
                    font-family: 'Cairo', 'Segoe UI', Tahoma, Arial, sans-serif;
                    direction: rtl;
                    background-color: #F8FAFC;
                    margin: 0;
                    padding: 16px;
                    color: #1E293B;
                  }
                  .brand-banner {
                    background-color: #0F766E;
                    color: #FFFFFF;
                    padding: 18px 20px;
                    border-radius: 8px;
                    text-align: center;
                    margin-bottom: 16px;
                  }
                  .brand-title {
                    font-size: 20pt;
                    font-weight: bold;
                    margin: 0 0 6px 0;
                    color: #FFFFFF;
                  }
                  .brand-sub {
                    font-size: 11pt;
                    color: #CCFBF1;
                    margin: 0;
                  }
                  .meta-bar {
                    width: 100%;
                    margin-bottom: 16px;
                    background-color: #FFFFFF;
                    border: 1px solid #E2E8F0;
                    border-radius: 6px;
                    padding: 10px 14px;
                    font-size: 10.5pt;
                  }
                  .kpi-table {
                    width: 100%;
                    margin-bottom: 20px;
                    border-collapse: separate;
                    border-spacing: 10px 0;
                  }
                  .kpi-cell {
                    padding: 14px 10px;
                    text-align: center;
                    border-radius: 8px;
                    vertical-align: middle;
                  }
                  .kpi-lana {
                    background-color: #FEF2F2;
                    border: 2px solid #F87171;
                  }
                  .kpi-laho {
                    background-color: #F0FDF4;
                    border: 2px solid #4ADE80;
                  }
                  .kpi-net {
                    background-color: $netBadgeBg;
                    border: 2px solid $netBadgeColor;
                  }
                  .kpi-count {
                    background-color: #F1F5F9;
                    border: 2px solid #94A3B8;
                  }
                  .data-table {
                    width: 100%;
                    border-collapse: collapse;
                    background-color: #FFFFFF;
                    border: 1px solid #CBD5E1;
                    font-size: 11pt;
                  }
                  .data-table th {
                    background-color: #0D9488;
                    color: #FFFFFF;
                    font-weight: bold;
                    padding: 12px 8px;
                    border: 1px solid #0F766E;
                    text-align: center;
                    font-size: 11.5pt;
                  }
                  .data-table td {
                    padding: 10px 8px;
                    border: 1px solid #E2E8F0;
                    vertical-align: middle;
                  }
                  .row-even {
                    background-color: #FFFFFF;
                  }
                  .row-odd {
                    background-color: #F8FAFC;
                  }
                  .account-name-cell {
                    text-align: right;
                    font-weight: bold;
                    font-size: 12.5pt;
                    color: #0F172A;
                    padding-right: 14px;
                  }
                  .net-lana-cell {
                    text-align: center;
                    font-weight: 900;
                    font-size: 13pt;
                    color: #991B1B;
                    background-color: #FEE2E2;
                    border: 1.5px solid #FCA5A5;
                  }
                  .net-laho-cell {
                    text-align: center;
                    font-weight: 900;
                    font-size: 13pt;
                    color: #166534;
                    background-color: #DCFCE7;
                    border: 1.5px solid #86EFAC;
                  }
                  .net-zero-cell {
                    text-align: center;
                    font-weight: bold;
                    font-size: 11.5pt;
                    color: #475569;
                    background-color: #F1F5F9;
                    border: 1px solid #CBD5E1;
                  }
                  .badge-lana {
                    background-color: #FECACA;
                    color: #991B1B;
                    font-weight: bold;
                    padding: 4px 10px;
                    border-radius: 12px;
                    font-size: 10pt;
                    display: inline-block;
                  }
                  .badge-laho {
                    background-color: #BBF7D0;
                    color: #166534;
                    font-weight: bold;
                    padding: 4px 10px;
                    border-radius: 12px;
                    font-size: 10pt;
                    display: inline-block;
                  }
                  .badge-zero {
                    background-color: #E2E8F0;
                    color: #475569;
                    padding: 4px 10px;
                    border-radius: 12px;
                    font-size: 10pt;
                    display: inline-block;
                  }
                  .footer-row {
                    background-color: #0F172A;
                    color: #FFFFFF;
                    font-weight: bold;
                    font-size: 12pt;
                  }
                  .footer-row td {
                    padding: 14px 10px;
                    border: 1px solid #334155;
                  }
                  .notice-box {
                    margin-top: 24px;
                    padding: 14px;
                    background-color: #F0FDFA;
                    border-right: 4px solid #0D9488;
                    border-radius: 4px;
                    font-size: 10pt;
                    color: #115E59;
                  }
                </style>
                </head>
                <body dir="rtl">
            """.trimIndent())

            // Header Banner
            appendLine("""
                <div class="brand-banner">
                  <div class="brand-title">📋 كشف الحسابات والأرصدة الصافية الشامل</div>
                  <div class="brand-sub">صادر عبر تطبيق البيان للمحاسبة السحابية | $storeName</div>
                </div>
            """.trimIndent())

            // Metadata Row
            appendLine("""
                <table class="meta-bar">
                  <tr>
                    <td style="text-align: right; width: 33%;"><b>📅 تاريخ ووقت التصدير:</b> $exportDate</td>
                    <td style="text-align: center; width: 33%;"><b>👥 إجمالي عدد الحسابات:</b> ${accounts.size} حساب</td>
                    <td style="text-align: left; width: 34%;"><b>🏪 المنشأة:</b> $storeName</td>
                  </tr>
                </table>
            """.trimIndent())

            // KPI Summary Dashboard Cards
            appendLine("""
                <table class="kpi-table">
                  <tr>
                    <!-- KPI 1: Total Lana -->
                    <td class="kpi-cell kpi-lana" style="width: 25%;">
                      <div style="font-size: 11pt; color: #991B1B; font-weight: bold;">إجمالي مطلوب لنا (مدين) 🔴</div>
                      <div style="font-size: 18pt; color: #DC2626; font-weight: 900; margin: 4px 0;">${formatMoney(totalLana)}</div>
                      <div style="font-size: 9.5pt; color: #B91C1C;">عدد الحسابات المدينة: $lanaCount</div>
                    </td>

                    <!-- KPI 2: Total Laho -->
                    <td class="kpi-cell kpi-laho" style="width: 25%;">
                      <div style="font-size: 11pt; color: #166534; font-weight: bold;">إجمالي مطلوب علينا (دائن) 🟢</div>
                      <div style="font-size: 18pt; color: #16A34A; font-weight: 900; margin: 4px 0;">${formatMoney(totalLaho)}</div>
                      <div style="font-size: 9.5pt; color: #15803D;">عدد الحسابات الدائنة: $lahoCount</div>
                    </td>

                    <!-- KPI 3: Net Overall -->
                    <td class="kpi-cell kpi-net" style="width: 32%;">
                      <div style="font-size: 11pt; color: $netBadgeColor; font-weight: bold;">صافي الرصيد العام الإجمالي 📊</div>
                      <div style="font-size: 19pt; color: $netBadgeColor; font-weight: 900; margin: 4px 0;">${formatMoney(kotlin.math.abs(overallNet))}</div>
                      <div style="font-size: 10pt; color: $netBadgeColor; font-weight: bold;">$netBadgeText</div>
                    </td>

                    <!-- KPI 4: Settled Accounts -->
                    <td class="kpi-cell kpi-count" style="width: 18%;">
                      <div style="font-size: 11pt; color: #475569; font-weight: bold;">حسابات مطابقة ⚪</div>
                      <div style="font-size: 18pt; color: #334155; font-weight: 900; margin: 4px 0;">$zeroCount</div>
                      <div style="font-size: 9.5pt; color: #64748B;">الرصيد خالص (0)</div>
                    </td>
                  </tr>
                </table>
            """.trimIndent())

            // Main Data Table
            appendLine("""
                <table class="data-table">
                  <thead>
                    <tr>
                      <th style="width: 40px;">م</th>
                      <th style="width: 220px;">اسم الحساب / العميل</th>
                      <th style="width: 90px;">التصنيف</th>
                      <th style="width: 120px;">رقم الهاتف</th>
                      <th style="width: 110px;">إجمالي لنا (+)</th>
                      <th style="width: 110px;">إجمالي له (-)</th>
                      <th style="width: 140px;">المبلغ الصافي</th>
                      <th style="width: 110px;">حالة الرصيد</th>
                      <th style="width: 70px;">العملة</th>
                      <th style="width: 100px;">ملاحظات</th>
                    </tr>
                  </thead>
                  <tbody>
            """.trimIndent())

            accounts.forEachIndexed { index, item ->
                val rowClass = if (index % 2 == 0) "row-even" else "row-odd"
                val num = index + 1
                val name = item.account.name
                val category = item.account.category
                val phone = item.account.phone.ifBlank { "-" }
                val cur = item.account.currency
                val lana = formatMoney(item.totalLana)
                val laho = formatMoney(item.totalLaho)
                val netVal = formatMoney(kotlin.math.abs(item.netBalance))

                val (netClass, statusBadge) = when {
                    item.netBalance > 0 -> Pair(
                        "net-lana-cell",
                        "<span class=\"badge-lana\">مطلوب منه (لنا)</span>"
                    )
                    item.netBalance < 0 -> Pair(
                        "net-laho-cell",
                        "<span class=\"badge-laho\">دائن له (علينا)</span>"
                    )
                    else -> Pair(
                        "net-zero-cell",
                        "<span class=\"badge-zero\">خالص ومطابق</span>"
                    )
                }

                appendLine("""
                    <tr class="$rowClass">
                      <td style="text-align: center; font-weight: bold; color: #64748B;">$num</td>
                      <td class="account-name-cell">$name</td>
                      <td style="text-align: center;"><span style="background-color: #E0F2FE; color: #0369A1; padding: 3px 8px; border-radius: 6px; font-size: 9.5pt; font-weight: bold;">$category</span></td>
                      <td style="text-align: center; direction: ltr; font-family: monospace; color: #475569;" mso-number-format="\@">$phone</td>
                      <td style="text-align: center; color: #DC2626; font-weight: bold;">$lana</td>
                      <td style="text-align: center; color: #16A34A; font-weight: bold;">$laho</td>
                      <td class="$netClass">$netVal</td>
                      <td style="text-align: center;">$statusBadge</td>
                      <td style="text-align: center; font-weight: bold; color: #334155;">$cur</td>
                      <td style="text-align: center; font-size: 9.5pt; color: #64748B;">${item.account.notes.ifBlank { "-" }}</td>
                    </tr>
                """.trimIndent())
            }

            // Summary Table Footer
            appendLine("""
                  </tbody>
                  <tfoot>
                    <tr class="footer-row">
                      <td colspan="4" style="text-align: right; padding: 14px 16px;">
                        المجموع الإجمالي لكافة الحسابات (${accounts.size} حساب)
                      </td>
                      <td style="text-align: center; color: #FCA5A5; font-size: 12.5pt;">${formatMoney(totalLana)}</td>
                      <td style="text-align: center; color: #86EFAC; font-size: 12.5pt;">${formatMoney(totalLaho)}</td>
                      <td style="text-align: center; color: ${if (overallNet > 0) "#FCA5A5" else if (overallNet < 0) "#86EFAC" else "#FFFFFF"}; font-size: 13.5pt; font-weight: 900;">
                        ${formatMoney(kotlin.math.abs(overallNet))}
                      </td>
                      <td colspan="3" style="text-align: center; color: #E2E8F0; font-size: 11pt;">
                        $netBadgeText
                      </td>
                    </tr>
                  </tfoot>
                </table>
            """.trimIndent())

            // Signature & Notice Box
            appendLine("""
                <div class="notice-box">
                  <b>ℹ️ إشعار وتوثيق المحاسبة:</b><br/>
                  • تم إنشاء هذا الكشف المالي وتدقيق أرصدته آلياً عبر <b>تطبيق البيان لإدارة الحسابات والديون</b>.<br/>
                  • الصيغة المحاسبية المعتمدة: <b>المبلغ الصافي = إجمالي لنا (مدين) - إجمالي له (دائن)</b>.<br/>
                  • تاريخ التصدير: $exportDate | حساب المنشأة: $storeName
                </div>
                </body>
                </html>
            """.trimIndent())
        }
    }

    private fun generateSingleAccountExcelHtml(
        account: AccountEntity,
        transactions: List<TransactionEntity>,
        totalLana: Double,
        totalLaho: Double,
        netBalance: Double,
        storeName: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val exportDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date())

        val (netBadgeColor, netBadgeBg, netStatusLabel) = when {
            netBalance > 0 -> Triple("#991B1B", "#FEE2E2", "مطلوب سداده (دين لنا على العميل)")
            netBalance < 0 -> Triple("#166534", "#DCFCE7", "رصيد دائن لصالح العميل (له بذمتنا)")
            else -> Triple("#334155", "#F1F5F9", "الحساب خالص ومطابق تماماً (0.00)")
        }

        return buildString {
            appendLine("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office"
                      xmlns:x="urn:schemas-microsoft-com:office:excel"
                      xmlns="http://www.w3.org/TR/REC-html40">
                <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                <!--[if gte mso 9]>
                <xml>
                 <x:ExcelWorkbook>
                  <x:ExcelWorksheets>
                   <x:ExcelWorksheet>
                    <x:Name>كشف حساب ${account.name}</x:Name>
                    <x:WorksheetOptions>
                     <x:DisplayRightToLeft/>
                     <x:DoNotDisplayGridlines/>
                    </x:WorksheetOptions>
                   </x:ExcelWorksheet>
                  </x:ExcelWorksheets>
                 </x:ExcelWorkbook>
                </xml>
                <![endif]-->
                <style>
                  body {
                    font-family: 'Cairo', 'Segoe UI', Tahoma, Arial, sans-serif;
                    direction: rtl;
                    background-color: #F8FAFC;
                    margin: 0;
                    padding: 16px;
                    color: #1E293B;
                  }
                  .brand-banner {
                    background-color: #0F766E;
                    color: #FFFFFF;
                    padding: 18px 20px;
                    border-radius: 8px;
                    text-align: center;
                    margin-bottom: 16px;
                  }
                  .brand-title {
                    font-size: 20pt;
                    font-weight: bold;
                    margin: 0 0 6px 0;
                    color: #FFFFFF;
                  }
                  .customer-card {
                    width: 100%;
                    margin-bottom: 16px;
                    background-color: #FFFFFF;
                    border: 1px solid #CBD5E1;
                    border-radius: 8px;
                    border-collapse: collapse;
                  }
                  .customer-card td {
                    padding: 10px 14px;
                    border: 1px solid #E2E8F0;
                    font-size: 11pt;
                  }
                  .kpi-table {
                    width: 100%;
                    margin-bottom: 20px;
                    border-collapse: separate;
                    border-spacing: 10px 0;
                  }
                  .kpi-cell {
                    padding: 14px 10px;
                    text-align: center;
                    border-radius: 8px;
                    vertical-align: middle;
                  }
                  .data-table {
                    width: 100%;
                    border-collapse: collapse;
                    background-color: #FFFFFF;
                    border: 1px solid #CBD5E1;
                    font-size: 11pt;
                  }
                  .data-table th {
                    background-color: #0D9488;
                    color: #FFFFFF;
                    font-weight: bold;
                    padding: 12px 8px;
                    border: 1px solid #0F766E;
                    text-align: center;
                    font-size: 11pt;
                  }
                  .data-table td {
                    padding: 10px 8px;
                    border: 1px solid #E2E8F0;
                    vertical-align: middle;
                  }
                  .row-even { background-color: #FFFFFF; }
                  .row-odd { background-color: #F8FAFC; }
                  .footer-row {
                    background-color: #0F172A;
                    color: #FFFFFF;
                    font-weight: bold;
                    font-size: 12pt;
                  }
                  .footer-row td {
                    padding: 14px 10px;
                    border: 1px solid #334155;
                  }
                </style>
                </head>
                <body dir="rtl">
            """.trimIndent())

            // Brand Banner
            appendLine("""
                <div class="brand-banner">
                  <div class="brand-title">📄 كشف حساب مالي تفصيلي</div>
                  <div style="font-size: 12pt; color: #CCFBF1;">تطبيق البيان للمحاسبة السحابية | $storeName</div>
                </div>
            """.trimIndent())

            // Customer Info Box
            appendLine("""
                <table class="customer-card">
                  <tr>
                    <td style="width: 25%; background-color: #F1F5F9; font-weight: bold;">اسم الحساب / العميل:</td>
                    <td style="width: 25%; font-size: 13pt; font-weight: 900; color: #0F172A;">${account.name}</td>
                    <td style="width: 25%; background-color: #F1F5F9; font-weight: bold;">رقم الهاتف:</td>
                    <td style="width: 25%; direction: ltr; font-family: monospace;">${account.phone.ifBlank { "غير مسجل" }}</td>
                  </tr>
                  <tr>
                    <td style="background-color: #F1F5F9; font-weight: bold;">تصنيف الحساب:</td>
                    <td>${account.category}</td>
                    <td style="background-color: #F1F5F9; font-weight: bold;">العملة المعتمدة:</td>
                    <td style="font-weight: bold; color: #0D9488;">${account.currency}</td>
                  </tr>
                  <tr>
                    <td style="background-color: #F1F5F9; font-weight: bold;">تاريخ التصدير:</td>
                    <td>$exportDate</td>
                    <td style="background-color: #F1F5F9; font-weight: bold;">كود الحساب:</td>
                    <td>#${account.id}</td>
                  </tr>
                </table>
            """.trimIndent())

            // KPI Summary Cards
            appendLine("""
                <table class="kpi-table">
                  <tr>
                    <td class="kpi-cell" style="width: 28%; background-color: #FEF2F2; border: 2px solid #F87171;">
                      <div style="font-size: 11pt; color: #991B1B; font-weight: bold;">إجمالي مشتريات / دين لنا (+)</div>
                      <div style="font-size: 18pt; color: #DC2626; font-weight: 900; margin: 4px 0;">${formatMoney(totalLana)} ${account.currency}</div>
                    </td>
                    <td class="kpi-cell" style="width: 28%; background-color: #F0FDF4; border: 2px solid #4ADE80;">
                      <div style="font-size: 11pt; color: #166534; font-weight: bold;">إجمالي دفعات / مسدد له (-)</div>
                      <div style="font-size: 18pt; color: #16A34A; font-weight: 900; margin: 4px 0;">${formatMoney(totalLaho)} ${account.currency}</div>
                    </td>
                    <td class="kpi-cell" style="width: 44%; background-color: $netBadgeBg; border: 2.5px solid $netBadgeColor;">
                      <div style="font-size: 11.5pt; color: $netBadgeColor; font-weight: bold;">المبلغ الصافي الحالي 💰</div>
                      <div style="font-size: 20pt; color: $netBadgeColor; font-weight: 900; margin: 4px 0;">${formatMoney(kotlin.math.abs(netBalance))} ${account.currency}</div>
                      <div style="font-size: 10pt; color: $netBadgeColor; font-weight: bold;">$netStatusLabel</div>
                    </td>
                  </tr>
                </table>
            """.trimIndent())

            // Transactions Table
            appendLine("""
                <table class="data-table">
                  <thead>
                    <tr>
                      <th style="width: 40px;">م</th>
                      <th style="width: 100px;">التاريخ</th>
                      <th style="width: 90px;">الوقت</th>
                      <th style="width: 110px;">نوع المعاملة</th>
                      <th style="width: 110px;">المبلغ</th>
                      <th style="width: 60px;">العملة</th>
                      <th style="width: 120px;">الرصيد التراكمي</th>
                      <th style="width: 240px;">البيان والشرح</th>
                      <th style="width: 90px;">رقم السند</th>
                    </tr>
                  </thead>
                  <tbody>
            """.trimIndent())

            var runningBalance = 0.0
            transactions.forEachIndexed { index, tx ->
                val rowClass = if (index % 2 == 0) "row-even" else "row-odd"
                val num = index + 1
                val isLana = tx.type == "LANA"
                if (isLana) runningBalance += tx.amount else runningBalance -= tx.amount

                val d = dateFormat.format(Date(tx.date))
                val t = timeFormat.format(Date(tx.date))
                val typeLabel = if (isLana) "دين لنا (+)" else "دفعة له (-)"
                val typeBg = if (isLana) "#FEE2E2" else "#DCFCE7"
                val typeColor = if (isLana) "#991B1B" else "#166534"
                val amtColor = if (isLana) "#DC2626" else "#16A34A"
                val desc = tx.description.ifBlank { if (isLana) "فاتورة مشتريات" else "سداد دفعة" }
                val receipt = tx.receiptNumber.ifBlank { "-" }

                val balColor = when {
                    runningBalance > 0 -> "#991B1B"
                    runningBalance < 0 -> "#166534"
                    else -> "#475569"
                }

                appendLine("""
                    <tr class="$rowClass">
                      <td style="text-align: center; font-weight: bold; color: #64748B;">$num</td>
                      <td style="text-align: center;">$d</td>
                      <td style="text-align: center; font-size: 9.5pt; color: #64748B;">$t</td>
                      <td style="text-align: center;"><span style="background-color: $typeBg; color: $typeColor; font-weight: bold; padding: 3px 8px; border-radius: 10px; font-size: 9.5pt;">$typeLabel</span></td>
                      <td style="text-align: center; font-weight: bold; color: $amtColor;">${formatMoney(tx.amount)}</td>
                      <td style="text-align: center;">${tx.currency}</td>
                      <td style="text-align: center; font-weight: 900; color: $balColor; background-color: #F8FAFC;">${formatMoney(kotlin.math.abs(runningBalance))}</td>
                      <td style="text-align: right; padding-right: 12px;">$desc</td>
                      <td style="text-align: center; font-family: monospace;">$receipt</td>
                    </tr>
                """.trimIndent())
            }

            // Table Footer
            appendLine("""
                  </tbody>
                  <tfoot>
                    <tr class="footer-row">
                      <td colspan="4" style="text-align: right; padding: 14px 16px;">إجمالي الحساب الصافي النهائي</td>
                      <td style="text-align: center; color: #FCA5A5; font-size: 12.5pt;">${formatMoney(totalLana)}</td>
                      <td>${account.currency}</td>
                      <td style="text-align: center; color: ${if (netBalance >= 0) "#FCA5A5" else "#86EFAC"}; font-size: 13.5pt; font-weight: 900;">
                        ${formatMoney(kotlin.math.abs(netBalance))}
                      </td>
                      <td colspan="2" style="text-align: center; color: #E2E8F0;">
                        $netStatusLabel
                      </td>
                    </tr>
                  </tfoot>
                </table>
                <div style="margin-top: 24px; padding: 14px; background-color: #F0FDFA; border-right: 4px solid #0D9488; border-radius: 4px; font-size: 10pt; color: #115E59;">
                  <b>✓ كشف حساب معتمد:</b> تم استخراج هذا الكشف وتحديث رصيد العميل <b>${account.name}</b> بنجاح عبر تطبيق البيان.
                </div>
                </body>
                </html>
            """.trimIndent())
        }
    }

    /**
     * Exports Top Purchasing Accounts to a beautifully styled, color-coded Excel spreadsheet (.xls)
     */
     fun exportTopPurchasingAccountsToExcel(
         context: Context,
         items: List<TopPurchaserItem>,
         storeName: String = "البيان"
     ) {
         try {
             val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
             val fileName = "تقرير_اكثر_الحسابات_شراء_البيان.xls"
             val file = File(exportDir, fileName)

             val exportDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date())
             val totalPurchasesAll = items.sumOf { it.totalPurchases }
             val totalInvoicesAll = items.sumOf { it.purchaseCount }
             val avgTicket = if (totalInvoicesAll > 0) totalPurchasesAll / totalInvoicesAll else 0.0

             val htmlContent = buildString {
                 appendLine("""
                    <!DOCTYPE html>
                    <html lang="ar" dir="rtl">
                    <head>
                    <meta charset="utf-8">
                    <title>تقرير أكثر الحسابات شراءً - تطبيق البيان</title>
                    <style>
                      body { font-family: 'Segoe UI', Tahoma, Arial, sans-serif; margin: 15px; background-color: #FFFFFF; color: #1E293B; }
                      .brand-banner { background: linear-gradient(135deg, #0284C7 0%, #0369A1 100%); color: #FFFFFF; padding: 18px; border-radius: 8px; text-align: center; margin-bottom: 16px; }
                      .brand-title { font-size: 18pt; font-weight: bold; margin-bottom: 4px; }
                      .kpi-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
                      .kpi-cell { padding: 14px; text-align: center; border-radius: 8px; }
                      .data-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                      .data-table th { background-color: #0F172A; color: #FFFFFF; padding: 10px 8px; font-size: 10.5pt; font-weight: bold; border: 1px solid #334155; }
                      .data-table td { padding: 9px 8px; font-size: 10.5pt; border: 1px solid #E2E8F0; text-align: right; }
                      .row-even { background-color: #F8FAFC; }
                      .row-odd { background-color: #FFFFFF; }
                      .rank-top1 { background-color: #FEF3C7; font-weight: bold; }
                      .rank-top2 { background-color: #F1F5F9; font-weight: bold; }
                      .rank-top3 { background-color: #FFEDD5; font-weight: bold; }
                      .notice-box { margin-top: 24px; padding: 14px; background-color: #F0FDFA; border-right: 4px solid #0D9488; border-radius: 4px; font-size: 10pt; color: #115E59; }
                    </style>
                    </head>
                    <body dir="rtl">
                      <div class="brand-banner">
                        <div class="brand-title">🏆 تقرير أكثر الحسابات شراءً وتعاملاً</div>
                        <div style="font-size: 12pt; color: #CCFBF1;">صادر عبر تطبيق البيان للمحاسبة السحابية | $storeName</div>
                      </div>

                      <table class="kpi-table">
                        <tr>
                          <td class="kpi-cell" style="width: 25%; background-color: #EFF6FF; border: 2px solid #60A5FA;">
                            <div style="font-size: 10.5pt; color: #1E40AF; font-weight: bold;">إجمالي المشتريات العامة</div>
                            <div style="font-size: 16pt; color: #1D4ED8; font-weight: 900; margin-top: 4px;">${formatMoney(totalPurchasesAll)}</div>
                          </td>
                          <td class="kpi-cell" style="width: 25%; background-color: #FEF3C7; border: 2px solid #FBBF24;">
                            <div style="font-size: 10.5pt; color: #92400E; font-weight: bold;">العميل المتصدر (#1)</div>
                            <div style="font-size: 14pt; color: #B45309; font-weight: 900; margin-top: 4px;">${items.firstOrNull()?.accountWithBalance?.account?.name ?: "-"}</div>
                          </td>
                          <td class="kpi-cell" style="width: 25%; background-color: #F0FDF4; border: 2px solid #4ADE80;">
                            <div style="font-size: 10.5pt; color: #166534; font-weight: bold;">إجمالي عدد الفواتير</div>
                            <div style="font-size: 16pt; color: #16A34A; font-weight: 900; margin-top: 4px;">$totalInvoicesAll فاتورة</div>
                          </td>
                          <td class="kpi-cell" style="width: 25%; background-color: #F5F3FF; border: 2px solid #A78BFA;">
                            <div style="font-size: 10.5pt; color: #5B21B6; font-weight: bold;">متوسط قيمة الفاتورة</div>
                            <div style="font-size: 16pt; color: #6D28D9; font-weight: 900; margin-top: 4px;">${formatMoney(avgTicket)}</div>
                          </td>
                        </tr>
                      </table>

                      <table class="data-table">
                        <thead>
                          <tr>
                            <th style="width: 50px; text-align: center;">الترتيب</th>
                            <th>اسم الحساب / العميل</th>
                            <th style="width: 100px; text-align: center;">التصنيف</th>
                            <th style="width: 120px; text-align: center;">رقم الهاتف</th>
                            <th style="width: 130px; text-align: center;">إجمالي المشتريات</th>
                            <th style="width: 90px; text-align: center;">عدد الفواتير</th>
                            <th style="width: 100px; text-align: center;">الحصة من المبيعات</th>
                            <th style="width: 120px; text-align: center;">الرصيد الصافي الحالي</th>
                            <th style="width: 60px; text-align: center;">العملة</th>
                          </tr>
                        </thead>
                        <tbody>
                 """.trimIndent())

                 items.forEach { item ->
                     val acc = item.accountWithBalance.account
                     val rankBadge = when (item.rank) {
                         1 -> "🥇 المركز 1"
                         2 -> "🥈 المركز 2"
                         3 -> "🥉 المركز 3"
                         else -> "#${item.rank}"
                     }
                     val rowClass = when (item.rank) {
                         1 -> "rank-top1"
                         2 -> "rank-top2"
                         3 -> "rank-top3"
                         else -> if (item.rank % 2 == 0) "row-even" else "row-odd"
                     }
                     val netBal = item.accountWithBalance.netBalance
                     val netColor = if (netBal > 0) "#DC2626" else if (netBal < 0) "#16A34A" else "#475569"
                     val netLabel = if (netBal > 0) "لنا: ${formatMoney(netBal)}" else if (netBal < 0) "له: ${formatMoney(-netBal)}" else "خالص (0.0)"

                     appendLine("""
                          <tr class="$rowClass">
                            <td style="text-align: center; font-weight: bold;">$rankBadge</td>
                            <td style="font-weight: bold; color: #0F172A;">${acc.name}</td>
                            <td style="text-align: center;">${acc.category}</td>
                            <td style="text-align: center; direction: ltr; font-family: monospace;">${acc.phone.ifBlank { "-" }}</td>
                            <td style="text-align: center; font-weight: 900; color: #0284C7; font-size: 11pt;">${formatMoney(item.totalPurchases)}</td>
                            <td style="text-align: center; font-weight: bold;">${item.purchaseCount}</td>
                            <td style="text-align: center; font-weight: bold; color: #4338CA;">${String.format(Locale.ENGLISH, "%.1f", item.purchaseSharePercent)}%</td>
                            <td style="text-align: center; font-weight: bold; color: $netColor;">$netLabel</td>
                            <td style="text-align: center;">${acc.currency}</td>
                          </tr>
                     """.trimIndent())
                 }

                 appendLine("""
                        </tbody>
                      </table>

                      <div class="notice-box">
                        <b>✓ تقرير معتمد:</b> تم استخراج تقرير أكثر العملاء شراءً وتعاملاً آلياً عبر <b>تطبيق البيان للمحاسبة السحابية</b> بتاريخ $exportDate.
                      </div>
                    </body>
                    </html>
                 """.trimIndent())
             }

             FileOutputStream(file).use { fos ->
                 OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                     writer.write("\uFEFF")
                     writer.write(htmlContent)
                 }
             }

             shareExcelFile(context, file, "تقرير أكثر الحسابات شراءً (Excel)")
             Toast.makeText(context, "تم تجهيز تقرير أكثر الحسابات شراءً بنجاح ✓", Toast.LENGTH_SHORT).show()
         } catch (e: Exception) {
             Toast.makeText(context, "تعذر تصدير التقرير: ${e.message}", Toast.LENGTH_LONG).show()
         }
     }

    /**
     * Exports Top Overdue / Delayed Paying Accounts to a beautifully styled, color-coded Excel spreadsheet (.xls)
     */
    fun exportOverdueAccountsToExcel(
        context: Context,
        items: List<OverdueAccountItem>,
        storeName: String = "البيان"
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "تقرير_اكثر_الحسابات_تاخرا_بالدفع_البيان.xls"
            val file = File(exportDir, fileName)

            val exportDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date())
            val totalOverdueAll = items.sumOf { it.overdueAmount }
            val maxDelayDays = items.maxOfOrNull { it.daysOverdue } ?: 0

            val htmlContent = buildString {
                appendLine("""
                    <!DOCTYPE html>
                    <html lang="ar" dir="rtl">
                    <head>
                    <meta charset="utf-8">
                    <title>تقرير أكثر الحسابات تأخراً بالدفع - تطبيق البيان</title>
                    <style>
                      body { font-family: 'Segoe UI', Tahoma, Arial, sans-serif; margin: 15px; background-color: #FFFFFF; color: #1E293B; }
                      .brand-banner { background: linear-gradient(135deg, #DC2626 0%, #991B1B 100%); color: #FFFFFF; padding: 18px; border-radius: 8px; text-align: center; margin-bottom: 16px; }
                      .brand-title { font-size: 18pt; font-weight: bold; margin-bottom: 4px; }
                      .kpi-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
                      .kpi-cell { padding: 14px; text-align: center; border-radius: 8px; }
                      .data-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                      .data-table th { background-color: #0F172A; color: #FFFFFF; padding: 10px 8px; font-size: 10.5pt; font-weight: bold; border: 1px solid #334155; }
                      .data-table td { padding: 9px 8px; font-size: 10.5pt; border: 1px solid #E2E8F0; text-align: right; }
                      .row-critical { background-color: #FEF2F2; }
                      .row-warning { background-color: #FFFBEB; }
                      .row-due { background-color: #F8FAFC; }
                      .badge-crit { background-color: #FEE2E2; color: #991B1B; font-weight: bold; padding: 4px 8px; border-radius: 6px; }
                      .badge-warn { background-color: #FEF3C7; color: #92400E; font-weight: bold; padding: 4px 8px; border-radius: 6px; }
                      .badge-due { background-color: #E2E8F0; color: #334155; font-weight: bold; padding: 4px 8px; border-radius: 6px; }
                      .notice-box { margin-top: 24px; padding: 14px; background-color: #FEF2F2; border-right: 4px solid #DC2626; border-radius: 4px; font-size: 10pt; color: #991B1B; }
                    </style>
                    </head>
                    <body dir="rtl">
                      <div class="brand-banner">
                        <div class="brand-title">⏰ تقرير أكثر الحسابات تأخراً بالدفع والديون المتعثرة</div>
                        <div style="font-size: 12pt; color: #FEE2E2;">صادر عبر تطبيق البيان للمحاسبة السحابية | $storeName</div>
                      </div>

                      <table class="kpi-table">
                        <tr>
                          <td class="kpi-cell" style="width: 25%; background-color: #FEF2F2; border: 2px solid #F87171;">
                            <div style="font-size: 10.5pt; color: #991B1B; font-weight: bold;">إجمالي الديون المتأخرة</div>
                            <div style="font-size: 16pt; color: #DC2626; font-weight: 900; margin-top: 4px;">${formatMoney(totalOverdueAll)}</div>
                          </td>
                          <td class="kpi-cell" style="width: 25%; background-color: #FFFBEB; border: 2px solid #FCD34D;">
                            <div style="font-size: 10.5pt; color: #92400E; font-weight: bold;">عدد الحسابات المتأخرة</div>
                            <div style="font-size: 16pt; color: #B45309; font-weight: 900; margin-top: 4px;">${items.size} عميل</div>
                          </td>
                          <td class="kpi-cell" style="width: 25%; background-color: #FEF2F2; border: 2px solid #EF4444;">
                            <div style="font-size: 10.5pt; color: #7F1D1D; font-weight: bold;">أطول مدة تأخير</div>
                            <div style="font-size: 16pt; color: #B91C1C; font-weight: 900; margin-top: 4px;">$maxDelayDays يوم</div>
                          </td>
                          <td class="kpi-cell" style="width: 25%; background-color: #EFF6FF; border: 2px solid #60A5FA;">
                            <div style="font-size: 10.5pt; color: #1E40AF; font-weight: bold;">أكبر مبلغ متأخر</div>
                            <div style="font-size: 16pt; color: #1D4ED8; font-weight: 900; margin-top: 4px;">${formatMoney(items.maxOfOrNull { it.overdueAmount } ?: 0.0)}</div>
                          </td>
                        </tr>
                      </table>

                      <table class="data-table">
                        <thead>
                          <tr>
                            <th style="width: 40px; text-align: center;">م</th>
                            <th>اسم الحساب / العميل</th>
                            <th style="width: 110px; text-align: center;">رقم الهاتف</th>
                            <th style="width: 120px; text-align: center;">مبلغ الدين المتأخر</th>
                            <th style="width: 60px; text-align: center;">العملة</th>
                            <th style="width: 120px; text-align: center;">مدة التأخير</th>
                            <th style="width: 120px; text-align: center;">حالة الاستحقاق</th>
                            <th style="width: 140px; text-align: center;">تاريخ آخر دفعة مسددة</th>
                          </tr>
                        </thead>
                        <tbody>
                """.trimIndent())

                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

                items.forEachIndexed { idx, item ->
                    val acc = item.accountWithBalance.account
                    val rowClass = when (item.severity) {
                        OverdueSeverity.CRITICAL -> "row-critical"
                        OverdueSeverity.WARNING -> "row-warning"
                        else -> "row-due"
                    }
                    val badgeClass = when (item.severity) {
                        OverdueSeverity.CRITICAL -> "badge-crit"
                        OverdueSeverity.WARNING -> "badge-warn"
                        else -> "badge-due"
                    }
                    val statusText = when (item.severity) {
                        OverdueSeverity.CRITICAL -> "🚨 تأخر حرج (+30 يوم)"
                        OverdueSeverity.WARNING -> "⚠️ متأخر (+15 يوم)"
                        OverdueSeverity.DUE -> "⏰ مستحق السداد"
                        OverdueSeverity.UPCOMING -> "⏳ يحل قريباً"
                    }

                    val lastPayText = if (item.lastPaymentDate != null && item.lastPaymentAmount != null) {
                        "${formatMoney(item.lastPaymentAmount)} بتاريخ ${dateFormat.format(Date(item.lastPaymentDate))}"
                    } else {
                        "لم تسجل دفعات سابقة"
                    }

                    appendLine("""
                          <tr class="$rowClass">
                            <td style="text-align: center; font-weight: bold; color: #64748B;">${idx + 1}</td>
                            <td style="font-weight: bold; color: #0F172A;">${acc.name}</td>
                            <td style="text-align: center; direction: ltr; font-family: monospace;">${acc.phone.ifBlank { "-" }}</td>
                            <td style="text-align: center; font-weight: 900; color: #DC2626; font-size: 11pt;">${formatMoney(item.overdueAmount)}</td>
                            <td style="text-align: center;">${acc.currency}</td>
                            <td style="text-align: center; font-weight: bold; color: #991B1B;">${item.daysOverdue} يوم تأخير</td>
                            <td style="text-align: center;"><span class="$badgeClass">$statusText</span></td>
                            <td style="text-align: center; font-size: 9.5pt; color: #475569;">$lastPayText</td>
                          </tr>
                    """.trimIndent())
                }

                appendLine("""
                        </tbody>
                      </table>

                      <div class="notice-box">
                        <b>⚠️ إشعار المطالبة والتحصيل:</b> تم تصدير كشف الحسابات الأكثر تأخراً بالدفع والديون المستحقة آلياً عبر <b>تطبيق البيان</b> بتاريخ $exportDate للمتابعة والتحصيل الفوري.
                      </div>
                    </body>
                    </html>
                """.trimIndent())
            }

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write(htmlContent)
                }
            }

            shareExcelFile(context, file, "تقرير أكثر الحسابات تأخراً بالدفع (Excel)")
            Toast.makeText(context, "تم تجهيز تقرير الحسابات المتأخرة بنجاح ✓", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر تصدير التقرير: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareExcelFile(context: Context, file: File, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = if (file.name.endsWith(".xls")) "application/vnd.ms-excel" else "text/csv"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title - تطبيق البيان للمحاسبة وإدارة الديون")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "فتح أو مشاركة كشف Excel").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        var res = value.replace("\r", " ").replace("\n", " ").replace("\"", "\"\"")
        if (res.contains(",") || res.contains("\"") || res.contains(";")) {
            res = "\"$res\""
        }
        return res
    }
}
