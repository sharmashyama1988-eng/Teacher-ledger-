package com.example.utils

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.Student
import com.example.data.FeeTransaction

object ExportHelper {

    /**
     * Generates standard CSV data for student ledger.
     * Excel and Google Sheets can load this natively.
     */
    fun generateStudentCsv(students: List<Student>): String {
        val csv = StringBuilder()
        // Header
        csv.append("Name,Roll Number,Phone,Total Fee,Paid Amount,Pending Balance,Payment Status,Remarks\n")
        
        for (s in students) {
            // Escape values containing commas to protect CSV structure
            val name = escapeCsv(s.name)
            val roll = escapeCsv(s.rollNum)
            val phone = escapeCsv(s.phone)
            val remarks = escapeCsv(s.remarks)
            val balance = s.totalFee - s.paidAmount

            csv.append("$name,$roll,$phone,${s.totalFee},${s.paidAmount},$balance,${s.status},$remarks\n")
        }
        return csv.toString()
    }

    /**
     * Generates CSV for fee receipts / transactions.
     */
    fun generateTransactionsCsv(transactions: List<FeeTransaction>): String {
        val csv = StringBuilder()
        csv.append("Transaction ID,Student Name,Amount Date,Month/Year,Payment Mode,Remarks\n")
        
        for (t in transactions) {
            val sName = escapeCsv(t.studentName)
            val pMode = escapeCsv(t.paymentMode)
            val rem = escapeCsv(t.remarks)
            val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(t.date))
            csv.append("${t.id},$sName,${t.amount},$formattedDate,${t.monthYear},$pMode,$rem\n")
        }
        return csv.toString()
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\n", " ").trim()
        if (str.contains(",") || str.contains("\"") || str.contains("'")) {
            str = str.replace("\"", "\"\"")
            str = "\"$str\""
        }
        return str
    }

    /**
     * Standard PDF generation pipeline using Android HTML-to-PDF print adapter.
     * Beautifully prints a summary report grid of the ledger using material style.
     */
    fun printLedgerToPdf(context: Context, students: List<Student>, totalEarnings: Double, totalPending: Double) {
        val htmlPage = StringBuilder()
        htmlPage.append("""
            <html>
            <head>
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; margin: 20px; }
                    h1 { color: #1a73e8; margin-bottom: 5px; }
                    .subtitle { color: #5f6368; font-size: 14px; margin-bottom: 25px; }
                    .stats-box { display: flex; justify-content: space-between; background: #f8f9fa; border-radius: 8px; padding: 15px; margin-bottom: 25px; border-left: 5px solid #1a73e8; }
                    .stat-item { flex: 1; }
                    .stat-label { font-size: 12px; color: #70757a; text-transform: uppercase; }
                    .stat-value { font-size: 20px; font-weight: bold; color: #202124; margin-top: 5px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th { background-color: #1a73e8; color: white; font-size: 13px; text-transform: uppercase; padding: 10px; text-align: left; }
                    td { padding: 10px; border-bottom: 1px solid #dadce0; font-size: 13px; }
                    tr:nth-child(even) { background-color: #f8f9fa; }
                    .status-paid { color: #137333; background: #e6f4ea; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }
                    .status-pending { color: #c5221f; background: #fce8e6; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }
                    .footer { font-size: 10px; color: #70757a; text-align: center; margin-top: 40px; border-top: 1px solid #dadce0; padding-top: 10px; }
                </style>
            </head>
            <body>
                <h1>Teacher Ledger - Students Report</h1>
                <div class="subtitle">Generated on ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}</div>
                
                <div class="stats-box">
                    <div class="stat-item">
                        <div class="stat-label">Total Students</div>
                        <div class="stat-value">${students.size}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Total Fee Collected</div>
                        <div class="stat-value">₹${totalEarnings}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Total Dues Pending</div>
                        <div class="stat-value">₹${totalPending}</div>
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Roll</th>
                            <th>Student Name</th>
                            <th>Total Fee</th>
                            <th>Paid</th>
                            <th>Pending</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        for (s in students) {
            val statusHtml = if (s.status == "PAID") {
                "<span class=\"status-paid\">Paid</span>"
            } else {
                "<span class=\"status-pending\">Pending</span>"
            }
            htmlPage.append("""
                <tr>
                    <td>${s.rollNum.ifEmpty { "-" }}</td>
                    <td><b>${s.name}</b><br/><small>${s.phone.ifEmpty { "No Phone" }}</small></td>
                    <td>₹${s.totalFee}</td>
                    <td>₹${s.paidAmount}</td>
                    <td>₹${s.balancePending}</td>
                    <td>$statusHtml</td>
                </tr>
            """.trimIndent())
        }

        htmlPage.append("""
                    </tbody>
                </table>
                <div class="footer">
                    Thank you for using Teacher Ledger - Simple Offline Tracking. Securely saved locally.
                </div>
            </body>
            </html>
        """.trimIndent())

        // Run on UI Thread to initialize WebView and dispatch to PrintManager
        context.runOnMainThread {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "TeacherLedger_Report_${System.currentTimeMillis()}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    
                    printManager.print(
                        jobName,
                        printAdapter,
                        PrintAttributes.Builder().build()
                    )
                }
            }
            webView.loadDataWithBaseURL(null, htmlPage.toString(), "text/html", "utf-8", null)
        }
    }
}

// Extension function to safely run operations on main thread from coroutines or background workers
fun Context.runOnMainThread(action: () -> Unit) {
    if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
        action()
    } else {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }
}
