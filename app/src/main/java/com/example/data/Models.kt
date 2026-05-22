package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rollNum: String = "",
    val phone: String = "",
    val remarks: String = "",
    val totalFee: Double = 0.0,
    val paidAmount: Double = 0.0,
    val lastPaymentDate: Long = 0L,
    val isSyncedToForm: Boolean = false,
    val syncTimestamp: Long = 0L
) {
    val balancePending: Double get() = (totalFee - paidAmount).coerceAtLeast(0.0)
    val status: String get() = if (balancePending <= 0.0 && totalFee > 0.0) "PAID" else "PENDING"
}

@Entity(tableName = "fee_transactions")
data class FeeTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val studentName: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val monthYear: String, // e.g. "May 2026"
    val paymentMode: String = "Cash", // Cash, UPI, Bank Transfer
    val remarks: String = ""
)

@Entity(tableName = "teacher_notes")
data class TeacherNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val linkedStudentId: Int? = null,
    val linkedStudentName: String? = null
)
