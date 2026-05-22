package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Students ---
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentByIdDirect(id: Int): Student?

    @Query("SELECT * FROM students WHERE id = :id")
    fun getStudentById(id: Int): Flow<Student?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    // --- Fee Transactions ---
    @Query("SELECT * FROM fee_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<FeeTransaction>>

    @Query("SELECT * FROM fee_transactions WHERE studentId = :studentId ORDER BY date DESC")
    fun getTransactionsForStudent(studentId: Int): Flow<List<FeeTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FeeTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: FeeTransaction)

    // --- Teacher Notes ---
    @Query("SELECT * FROM teacher_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<TeacherNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TeacherNote): Long

    @Update
    suspend fun updateNote(note: TeacherNote)

    @Delete
    suspend fun deleteNote(note: TeacherNote)
}
