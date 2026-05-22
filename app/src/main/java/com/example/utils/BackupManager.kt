package com.example.utils

import com.example.data.FeeTransaction
import com.example.data.Student
import com.example.data.TeacherNote
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val students: List<Student>,
    val transactions: List<FeeTransaction>,
    val notes: List<TeacherNote>
)

object BackupManager {

    /**
     * Converts database lists into a secure, single JSON string for backup.
     */
    fun createBackupJson(
        students: List<Student>,
        transactions: List<FeeTransaction>,
        notes: List<TeacherNote>
    ): String {
        return try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())

            // Students array
            val studentsArr = JSONArray()
            for (s in students) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("rollNum", s.rollNum)
                obj.put("phone", s.phone)
                obj.put("remarks", s.remarks)
                obj.put("totalFee", s.totalFee)
                obj.put("paidAmount", s.paidAmount)
                obj.put("lastPaymentDate", s.lastPaymentDate)
                obj.put("isSyncedToForm", s.isSyncedToForm)
                obj.put("syncTimestamp", s.syncTimestamp)
                studentsArr.put(obj)
            }
            root.put("students", studentsArr)

            // Transactions array
            val txArr = JSONArray()
            for (t in transactions) {
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("studentId", t.studentId)
                obj.put("studentName", t.studentName)
                obj.put("amount", t.amount)
                obj.put("date", t.date)
                obj.put("monthYear", t.monthYear)
                obj.put("paymentMode", t.paymentMode)
                obj.put("remarks", t.remarks)
                txArr.put(obj)
            }
            root.put("transactions", txArr)

            // Notes array
            val notesArr = JSONArray()
            for (n in notes) {
                val obj = JSONObject()
                obj.put("id", n.id)
                obj.put("title", n.title)
                obj.put("content", n.content)
                obj.put("createdAt", n.createdAt)
                obj.put("linkedStudentId", n.linkedStudentId ?: -1)
                obj.put("linkedStudentName", n.linkedStudentName ?: "")
                notesArr.put(obj)
            }
            root.put("notes", notesArr)

            root.toString(2) // Pretty printing with indent
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Parses a backup JSON string and returns structured objects to restore.
     */
    fun parseBackupJson(jsonStr: String): BackupData? {
        return try {
            val root = JSONObject(jsonStr)
            val studentsList = mutableListOf<Student>()
            val txList = mutableListOf<FeeTransaction>()
            val notesList = mutableListOf<TeacherNote>()

            if (root.has("students")) {
                val arr = root.getJSONArray("students")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    studentsList.add(
                        Student(
                            id = 0, // Reset to 0 to trigger fresh Room insertion safely
                            name = obj.getString("name"),
                            rollNum = obj.optString("rollNum", ""),
                            phone = obj.optString("phone", ""),
                            remarks = obj.optString("remarks", ""),
                            totalFee = obj.optDouble("totalFee", 0.0),
                            paidAmount = obj.optDouble("paidAmount", 0.0),
                            lastPaymentDate = obj.optLong("lastPaymentDate", 0L),
                            isSyncedToForm = obj.optBoolean("isSyncedToForm", false),
                            syncTimestamp = obj.optLong("syncTimestamp", 0L)
                        )
                    )
                }
            }

            if (root.has("transactions")) {
                val arr = root.getJSONArray("transactions")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    txList.add(
                        FeeTransaction(
                            id = 0,
                            studentId = obj.optInt("studentId", 0),
                            studentName = obj.optString("studentName", "Student"),
                            amount = obj.optDouble("amount", 0.0),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            monthYear = obj.optString("monthYear", "Ongoing"),
                            paymentMode = obj.optString("paymentMode", "Cash"),
                            remarks = obj.optString("remarks", "")
                        )
                    )
                }
            }

            if (root.has("notes")) {
                val arr = root.getJSONArray("notes")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val sId = obj.optInt("linkedStudentId", -1)
                    val sName = obj.optString("linkedStudentName", "")
                    notesList.add(
                        TeacherNote(
                            id = 0,
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            linkedStudentId = if (sId != -1) sId else null,
                            linkedStudentName = if (sName.isNotEmpty()) sName else null
                        )
                    )
                }
            }

            BackupData(studentsList, txList, notesList)
        } catch (e: Exception) {
            null
        }
    }
}
