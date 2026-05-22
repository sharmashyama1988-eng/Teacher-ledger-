package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.FeeTransaction
import com.example.data.Student
import com.example.data.TeacherNote
import com.example.utils.BackupManager
import com.example.utils.ExportHelper
import com.example.utils.GoogleFormSync
import com.example.utils.SettingsManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val repository: AppRepository,
    val settings: SettingsManager
) : ViewModel() {

    // --- Search & Filters ---
    private val _studentSearchQuery = MutableStateFlow("")
    val studentSearchQuery = _studentSearchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("ALL") // ALL, PAID, PENDING
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery = _noteSearchQuery.asStateFlow()

    // --- Settings UI State ---
    private val _isDarkMode = MutableStateFlow(settings.isDarkMode)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _isPinLocked = MutableStateFlow(settings.isPinLockEnabled && settings.hashedPinCode.isNotEmpty())
    val isPinLocked = _isPinLocked.asStateFlow()

    // --- Sync & Export Progress States ---
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    // --- Core Data Streams ---
    val allStudents: StateFlow<List<Student>> = repository.allStudents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<FeeTransaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allNotes: StateFlow<List<TeacherNote>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Filtered Students ---
    val filteredStudents: StateFlow<List<Student>> = combine(
        allStudents,
        studentSearchQuery,
        selectedStatusFilter
    ) { students, query, filter ->
        students.filter { s ->
            val matchesQuery = s.name.contains(query, ignoreCase = true) || s.rollNum.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "PAID" -> s.status == "PAID"
                "PENDING" -> s.status == "PENDING"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filtered Notes ---
    val filteredNotes: StateFlow<List<TeacherNote>> = combine(
        allNotes,
        noteSearchQuery
    ) { notes, query ->
        notes.filter { n ->
            n.title.contains(query, ignoreCase = true) || n.content.contains(query, ignoreCase = true) || (n.linkedStudentName?.contains(query, ignoreCase = true) ?: false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Monthly Earnings Metrics ---
    val monthlyEarnings: StateFlow<Map<String, Double>> = allTransactions
        .combine(allStudents) { txs, _ ->
            val map = mutableMapOf<String, Double>()
            for (t in txs) {
                map[t.monthYear] = (map[t.monthYear] ?: 0.0) + t.amount
            }
            map
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Total Portfolio Financial Sums ---
    val portfolioSums: StateFlow<PortfolioSummary> = allStudents
        .combine(allTransactions) { students, _ ->
            var totalRequired = 0.0
            var totalCollected = 0.0
            var paidCount = 0
            var pendingCount = 0

            for (s in students) {
                totalRequired += s.totalFee
                totalCollected += s.paidAmount
                if (s.status == "PAID") paidCount++ else pendingCount++
            }

            PortfolioSummary(
                totalRequiredFees = totalRequired,
                totalCollectedFees = totalCollected,
                totalPendingDues = (totalRequired - totalCollected).coerceAtLeast(0.0),
                paidStudentsCount = paidCount,
                pendingStudentsCount = pendingCount
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioSummary())

    // --- Core Operations ---
    fun setSearchQuery(query: String) {
        _studentSearchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _selectedStatusFilter.value = filter
    }

    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    fun toggleDarkMode(enabled: Boolean) {
        settings.isDarkMode = enabled
        _isDarkMode.value = enabled
    }

    // --- Authentication Actions ---
    fun verifyPinAndUnlock(pin: String): Boolean {
        val hashed = com.example.utils.SecurityHelper.hashPin(pin)
        val valid = hashed == settings.hashedPinCode
        if (valid) {
            _isPinLocked.value = false
        }
        return valid
    }

    fun setupNewPin(pin: String) {
        val hashed = com.example.utils.SecurityHelper.hashPin(pin)
        settings.hashedPinCode = hashed
        settings.isPinLockEnabled = true
        _isPinLocked.value = false
    }

    fun disablePinLock() {
        settings.hashedPinCode = ""
        settings.isPinLockEnabled = false
        _isPinLocked.value = false
    }

    // --- Student Actions ---
    fun addStudent(name: String, rollNum: String, phone: String, remarks: String, totalFee: Double) {
        viewModelScope.launch {
            val student = Student(
                name = name,
                rollNum = rollNum,
                phone = phone,
                remarks = remarks,
                totalFee = totalFee,
                paidAmount = 0.0,
                lastPaymentDate = 0L
            )
            val generatedId = repository.insertStudent(student)
            
            // Auto Google Form Sync if enabled
            if (settings.isGoogleFormSyncEnabled) {
                syncSingleStudentDirectly(student.copy(id = generatedId.toInt()))
            }
        }
    }

    fun updateStudentDetails(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)

            // Auto Google Form Sync if enabled
            if (settings.isGoogleFormSyncEnabled) {
                syncSingleStudentDirectly(student)
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    // --- Payment Log Actions ---
    fun addPaymentTransaction(studentId: Int, studentName: String, amount: Double, mode: String, remarks: String) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
            val currentMonthYear = dateFormat.format(Date())

            val tx = FeeTransaction(
                studentId = studentId,
                studentName = studentName,
                amount = amount,
                monthYear = currentMonthYear,
                paymentMode = mode,
                remarks = remarks
            )
            repository.addFeeTransaction(tx)

            // Fetch absolute updated student record to sync accurate metrics
            val updatedStudent = repository.getStudentByIdDirect(studentId)
            if (updatedStudent != null && settings.isGoogleFormSyncEnabled) {
                syncSingleStudentDirectly(updatedStudent)
            }
        }
    }

    fun removePaymentTransaction(tx: FeeTransaction) {
        viewModelScope.launch {
            repository.removeFeeTransaction(tx)
        }
    }

    // --- Notes Actions ---
    fun addTeacherNote(title: String, content: String, linkedStudent: Student? = null) {
        viewModelScope.launch {
            val note = TeacherNote(
                title = title,
                content = content,
                linkedStudentId = linkedStudent?.id,
                linkedStudentName = linkedStudent?.name
            )
            repository.insertNote(note)
        }
    }

    fun editTeacherNote(note: TeacherNote) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteTeacherNote(note: TeacherNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // --- Google Form Async Trigger ---
    fun syncSingleStudentDirectly(student: Student) {
        viewModelScope.launch {
            _syncStatus.value = "Syncing ${student.name}..."
            val result = GoogleFormSync.syncStudentToForm(student, settings)
            result.fold(
                onSuccess = {
                    repository.updateStudent(student.copy(isSyncedToForm = true, syncTimestamp = System.currentTimeMillis()))
                    _syncStatus.value = "Synced ${student.name} successfully!"
                },
                onFailure = { error ->
                    _syncStatus.value = "Failed to sync: ${error.localizedMessage}"
                }
            )
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    // --- CSV Share & Print PDF Actions ---
    fun intentShareCsv(context: Context, mode: String) { // "STUDENTS" or "TRANSACTIONS"
        viewModelScope.launch {
            val csvContent = if (mode == "STUDENTS") {
                ExportHelper.generateStudentCsv(allStudents.value)
            } else {
                ExportHelper.generateTransactionsCsv(allTransactions.value)
            }

            val title = if (mode == "STUDENTS") "StudentsLedger.csv" else "EarningsLedger.csv"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, csvContent)
            }
            context.startActivity(Intent.createChooser(intent, "Share Ledger File to Excel"))
        }
    }

    fun triggerPrintPdf(context: Context) {
        val sums = portfolioSums.value
        ExportHelper.printLedgerToPdf(context, allStudents.value, sums.totalCollectedFees, sums.totalPendingDues)
    }

    // --- Backup Operations ---
    fun performBackupShare(context: Context) {
        val backupStr = BackupManager.createBackupJson(allStudents.value, allTransactions.value, allNotes.value)
        if (backupStr.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "TeacherLedger_Backup.json")
                putExtra(Intent.EXTRA_TEXT, backupStr)
            }
            context.startActivity(Intent.createChooser(intent, "Share Secure Backup File (JSON)"))
        }
    }

    // --- Gemini AI Assistant States & Logic ---
    private val _aiGenerationLoading = MutableStateFlow(false)
    val aiGenerationLoading = _aiGenerationLoading.asStateFlow()

    private val _aiGenerationResult = MutableStateFlow<String?>(null)
    val aiGenerationResult = _aiGenerationResult.asStateFlow()

    fun generateWithGemini(prompt: String) {
        viewModelScope.launch {
            _aiGenerationLoading.value = true
            _aiGenerationResult.value = "Thinking..."
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _aiGenerationResult.value = "Error: Gemini API Key is empty or placeholder. Please set your actual API Key in the AI Studio Secets panel as GEMINI_API_KEY."
                    _aiGenerationLoading.value = false
                    return@launch
                }

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val contentJson = org.json.JSONObject().apply {
                    val contentsArray = org.json.JSONArray().apply {
                        val contentObj = org.json.JSONObject().apply {
                            val partsArray = org.json.JSONArray().apply {
                                val partObj = org.json.JSONObject().apply {
                                    put("text", prompt)
                                }
                                put(partObj)
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = contentJson.toString().toRequestBody(mediaType)

                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val respBody = response.body?.string()
                        if (response.isSuccessful && respBody != null) {
                            try {
                                val jsonObject = org.json.JSONObject(respBody)
                                val candidates = jsonObject.getJSONArray("candidates")
                                val firstCandidate = candidates.getJSONObject(0)
                                val contentObj = firstCandidate.getJSONObject("content")
                                val partsArray = contentObj.getJSONArray("parts")
                                val firstPart = partsArray.getJSONObject(0)
                                val textResult = firstPart.getString("text")
                                _aiGenerationResult.value = textResult
                            } catch (e: Exception) {
                                _aiGenerationResult.value = "AI Response parsing failed: ${e.message}\nRaw: $respBody"
                            }
                        } else {
                            _aiGenerationResult.value = "API Call Failed: Code ${response.code}\nResponse: ${respBody ?: "No body text"}"
                        }
                    }
                }
            } catch (e: Exception) {
                _aiGenerationResult.value = "Error generating AI response: ${e.message ?: "Unknown Connection Error"}"
            } finally {
                _aiGenerationLoading.value = false
            }
        }
    }

    fun clearAiResult() {
        _aiGenerationResult.value = null
    }

    fun performRestore(backupJson: String): Boolean {
        val data = BackupManager.parseBackupJson(backupJson)
        return if (data != null) {
            viewModelScope.launch {
                repository.restoreAllData(data.students, data.transactions, data.notes)
            }
            true
        } else {
            false
        }
    }
}

class MainViewModelFactory(
    private val repository: AppRepository,
    private val settings: SettingsManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, settings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class PortfolioSummary(
    val totalRequiredFees: Double = 0.0,
    val totalCollectedFees: Double = 0.0,
    val totalPendingDues: Double = 0.0,
    val paidStudentsCount: Int = 0,
    val pendingStudentsCount: Int = 0
)
