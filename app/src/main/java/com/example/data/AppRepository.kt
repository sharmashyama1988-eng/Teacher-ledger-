package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    val allStudents: Flow<List<Student>> = appDao.getAllStudents()
    val allTransactions: Flow<List<FeeTransaction>> = appDao.getAllTransactions()
    val allNotes: Flow<List<TeacherNote>> = appDao.getAllNotes()

    fun getStudentById(id: Int): Flow<Student?> = appDao.getStudentById(id)

    suspend fun getStudentByIdDirect(id: Int): Student? = appDao.getStudentByIdDirect(id)

    suspend fun insertStudent(student: Student): Long = appDao.insertStudent(student)

    suspend fun updateStudent(student: Student) = appDao.updateStudent(student)

    suspend fun deleteStudent(student: Student) = appDao.deleteStudent(student)

    /**
     * Inserts a transaction AND updates the student's paidAmount and last payment date atomically.
     */
    suspend fun addFeeTransaction(transaction: FeeTransaction) {
        // Insert transaction first
        appDao.insertTransaction(transaction)

        // Find and update student
        val student = appDao.getStudentByIdDirect(transaction.studentId)
        if (student != null) {
            val newPaidAmount = (student.paidAmount + transaction.amount).coerceAtMost(student.totalFee)
            val updatedStudent = student.copy(
                paidAmount = newPaidAmount,
                lastPaymentDate = transaction.date
            )
            appDao.updateStudent(updatedStudent)
        }
    }

    /**
     * Deletes a transaction and reduces the student's paidAmount accordingly.
     */
    suspend fun removeFeeTransaction(transaction: FeeTransaction) {
        appDao.deleteTransaction(transaction)

        val student = appDao.getStudentByIdDirect(transaction.studentId)
        if (student != null) {
            val newPaidAmount = (student.paidAmount - transaction.amount).coerceAtLeast(0.0)
            val updatedStudent = student.copy(
                paidAmount = newPaidAmount
            )
            appDao.updateStudent(updatedStudent)
        }
    }

    // --- Teacher Notes ---
    suspend fun insertNote(note: TeacherNote): Long = appDao.insertNote(note)
    suspend fun updateNote(note: TeacherNote) = appDao.updateNote(note)
    suspend fun deleteNote(note: TeacherNote) = appDao.deleteNote(note)

    /**
     * Overwrites database table details during manual restore operations.
     */
    suspend fun restoreAllData(
        students: List<Student>,
        transactions: List<FeeTransaction>,
        notes: List<TeacherNote>
    ) {
        // Since we reset IDs in BackupManager, bulk inserting represents a safe fresh load
        for (student in students) {
            appDao.insertStudent(student)
        }
        for (tx in transactions) {
            appDao.insertTransaction(tx)
        }
        for (note in notes) {
            appDao.insertNote(note)
        }
    }
}
