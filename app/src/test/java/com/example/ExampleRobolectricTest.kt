package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.utils.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AppDao
    private lateinit var repository: AppRepository
    private lateinit var viewModel: MainViewModel
    private lateinit var settings: SettingsManager
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.appDao()
        repository = AppRepository(dao)
        settings = SettingsManager(context)
        viewModel = MainViewModel(repository, settings)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testAppTitle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Teacher Ledger", appName)
    }

    @Test
    fun testDatabaseAndViewModelFlow() = runTest {
        // 1. Initial states of students should be empty
        var students = viewModel.allStudents.value
        assertTrue(students.isEmpty())

        // 2. Add a student using repository
        val testStudent = Student(
            id = 1,
            name = "John Doe",
            rollNum = "101",
            phone = "1234567890",
            totalFee = 5000.0,
            paidAmount = 2000.0
        )
        dao.insertStudent(testStudent)

        // Wait to gather from flow
        students = viewModel.allStudents.first { it.isNotEmpty() }
        assertEquals(1, students.size)
        assertEquals("John Doe", students[0].name)
        assertEquals(3000.0, students[0].balancePending, 0.01)

        // 3. Test Portfolio Sums calculation
        val portfolio = viewModel.portfolioSums.first { it.totalRequiredFees > 0 }
        assertEquals(5000.0, portfolio.totalRequiredFees, 0.01)
        assertEquals(2000.0, portfolio.totalCollectedFees, 0.01)
        assertEquals(3000.0, portfolio.totalPendingDues, 0.01)
    }

    @Test
    fun testNotesSearchQuery() = runTest {
        val note1 = TeacherNote(
            id = 1,
            title = "Math Lesson Plan",
            content = "Introduction to Calculus",
            createdAt = System.currentTimeMillis()
        )
        val note2 = TeacherNote(
            id = 2,
            title = "Weekly Exam",
            content = "Algebra portion details",
            createdAt = System.currentTimeMillis()
        )

        dao.insertNote(note1)
        dao.insertNote(note2)

        // Verify loaded notes
        val initialNotes = viewModel.allNotes.first { it.size == 2 }
        assertEquals(2, initialNotes.size)

        // Filter notes by search query
        viewModel.setNoteSearchQuery("Calculus")
        val filtered = viewModel.filteredNotes.first { it.size == 1 }
        assertEquals(1, filtered.size)
        assertEquals("Math Lesson Plan", filtered[0].title)
    }
}
