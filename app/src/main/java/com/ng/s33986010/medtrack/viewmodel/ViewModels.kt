package com.ng.s33986010.medtrack.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ng.s33986010.medtrack.data.repository.MedTrackRepository
import com.ng.s33986010.medtrack.model.*
import com.ng.s33986010.medtrack.ui.screens.SymptomTrendViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// SHARED APPLICATION VIEWMODEL FACTORY
class MedTrackViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = MedTrackRepository(app.applicationContext)
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repo) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repo) as T
            modelClass.isAssignableFrom(SymptomsViewModel::class.java) -> SymptomsViewModel(repo) as T
            modelClass.isAssignableFrom(MedCoachViewModel::class.java) -> MedCoachViewModel(repo) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repo) as T
            modelClass.isAssignableFrom(ClinicianViewModel::class.java) -> ClinicianViewModel(repo) as T
            modelClass.isAssignableFrom(SymptomTrendViewModel::class.java) -> SymptomTrendViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

// AUTHVIEWMODEL
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val patientId: String) : AuthState()
    data class Error(val message: String) : AuthState()
    object LoggedOut : AuthState()
}

class AuthViewModel(private val repo: MedTrackRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun checkSession() {
        val id = repo.getLoggedInPatientId()
        if (!id.isNullOrBlank()) {
            _authState.value = AuthState.Success(id)
        }
    }

    fun seedDatabaseIfNeeded() {
        viewModelScope.launch {
            if (!repo.isDbSeeded()) {
                repo.seedFromCsv()
            }
        }
    }

    /** Account claiming: PatientID + PhoneNumber → set new password */
    fun claimAccount(patientId: String, phone: String, newPassword: String) {
        if (patientId.isBlank() || phone.isBlank() || newPassword.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val patient = repo.getPatientByIdAndPhone(patientId.trim(), phone.trim())
            when {
                patient == null -> _authState.value = AuthState.Error("No patient found with that ID and phone number")
                patient.password != null -> _authState.value = AuthState.Error("Account already claimed. Please login.")
                newPassword.length < 6 -> _authState.value = AuthState.Error("Password must be at least 6 characters")
                else -> {
                    repo.updatePatient(patient.copy(password = newPassword))
                    repo.saveSession(patient.patientId)
                    _authState.value = AuthState.Success(patient.patientId)
                }
            }
        }
    }

    /** Login: PatientID + Password */
    fun login(patientId: String, password: String) {
        if (patientId.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Patient ID and password are required")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val patient = repo.getPatientByIdAndPassword(patientId.trim(), password)
            if (patient != null) {
                repo.saveSession(patient.patientId)
                _authState.value = AuthState.Success(patient.patientId)
            } else {
                _authState.value = AuthState.Error("Invalid Patient ID or password")
            }
        }
    }

    /** Register new user */
    fun signUp(name: String, phone: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when {
                name.isBlank() -> { _authState.value = AuthState.Error("Name is required"); return@launch }
                phone.isBlank() -> { _authState.value = AuthState.Error("Phone number is required"); return@launch }
                password.length < 6 -> { _authState.value = AuthState.Error("Password must be at least 6 characters"); return@launch }
                password != confirmPassword -> { _authState.value = AuthState.Error("Passwords do not match"); return@launch }
            }
            val newId = repo.generateNewPatientId()
            repo.insertNewPatient(PatientEntity(
                patientId = newId,
                phoneNumber = phone.trim(),
                name = name.trim(),
                password = password
            ))
            repo.saveSession(newId)
            _authState.value = AuthState.Success(newId)
        }
    }

    fun logout() {
        repo.clearSession()
        _authState.value = AuthState.LoggedOut
    }

    fun resetState() { _authState.value = AuthState.Idle }
}

// HOMEVIEWMODEL
class HomeViewModel(private val repo: MedTrackRepository) : ViewModel() {

    private val _patientId = MutableStateFlow("")
    private val _patient = MutableStateFlow<PatientEntity?>(null)
    val patient: StateFlow<PatientEntity?> = _patient

    val medications: StateFlow<List<MedicationEntity>> = _patientId
        .filter { it.isNotBlank() }
        .flatMapLatest { repo.getMedicationsFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map<medicationId, isTaken>
    private val _takenMap = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val takenMap: StateFlow<Map<Int, Boolean>> = _takenMap

    private val _saveState = MutableStateFlow<String?>(null)
    val saveState: StateFlow<String?> = _saveState

    fun init(patientId: String) {
        _patientId.value = patientId
        viewModelScope.launch {
            _patient.value = repo.getPatientById(patientId)
        }
    }

    fun refreshTakenStatus(medIds: List<Int>) {
        viewModelScope.launch {
            _takenMap.value = repo.getTakenStatusForMeds(medIds)
        }
    }

    fun setTaken(medicationId: Int, taken: Boolean) {
        viewModelScope.launch {
            repo.setTakenStatus(medicationId, taken)
            _takenMap.value = _takenMap.value.toMutableMap().also { it[medicationId] = taken }
        }
    }

    fun addMedication(
        name: String, dosage: String, frequency: String,
        time: String, type: String, notes: String
    ) {
        val patientId = _patientId.value
        if (patientId.isBlank()) return
        viewModelScope.launch {
            repo.addMedication(MedicationEntity(
                patientId = patientId,
                medicationName = name.trim(),
                dosage = dosage.trim(),
                frequency = frequency,
                scheduledTime = time.trim(),
                medicationType = type,
                notes = notes.trim()
            ))
            _saveState.value = "Medication saved successfully"
        }
    }

    fun clearSaveState() { _saveState.value = null }
}

// SYMPTOMS VIEW MODEL
class SymptomsViewModel(private val repo: MedTrackRepository) : ViewModel() {

    private val _patientId = MutableStateFlow("")

    val symptoms: StateFlow<List<SymptomEntity>> = _patientId
        .filter { it.isNotBlank() }
        .flatMapLatest { repo.getSymptomsFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveState = MutableStateFlow<String?>(null)
    val saveState: StateFlow<String?> = _saveState

    fun init(patientId: String) { _patientId.value = patientId }

    fun addSymptom(category: String, severity: Int, notes: String, dateTime: String) {
        viewModelScope.launch {
            repo.addSymptom(SymptomEntity(
                patientId = _patientId.value,
                category = category,
                severity = severity,
                notes = notes,
                dateTime = dateTime
            ))
            _saveState.value = "Symptom saved successfully"
        }
    }

    fun clearSaveState() { _saveState.value = null }
}

// MED COACH VIEW MODEL
sealed class DrugSearchState {
    object Idle : DrugSearchState()
    object Loading : DrugSearchState()
    data class Success(val result: com.ng.s33986010.medtrack.network.DrugLabelResult) : DrugSearchState()
    data class Error(val message: String) : DrugSearchState()
}

sealed class TipState {
    object Idle : TipState()
    object Loading : TipState()
    data class Success(val tip: String) : TipState()
    data class Error(val message: String) : TipState()
}

class MedCoachViewModel(private val repo: MedTrackRepository) : ViewModel() {

    private val _patientId = MutableStateFlow("")

    private val _drugState = MutableStateFlow<DrugSearchState>(DrugSearchState.Idle)
    val drugState: StateFlow<DrugSearchState> = _drugState

    private val _tipState = MutableStateFlow<TipState>(TipState.Idle)
    val tipState: StateFlow<TipState> = _tipState

    val tips: StateFlow<List<MedCoachTipEntity>> = _patientId
        .filter { it.isNotBlank() }
        .flatMapLatest { repo.getTipsFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _patientMedNames = MutableStateFlow<List<String>>(emptyList())
    val patientMedNames: StateFlow<List<String>> = _patientMedNames

    fun init(patientId: String, geminiApiKey: String) {
        _patientId.value = patientId
        _geminiKey = geminiApiKey
        viewModelScope.launch {
            _patientMedNames.value = repo.getMedications(patientId).map { it.medicationName }
        }
    }

    private var _geminiKey = ""

    fun searchDrug(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _drugState.value = DrugSearchState.Loading
            repo.searchDrug(name).fold(
                onSuccess = { _drugState.value = DrugSearchState.Success(it) },
                onFailure = { _drugState.value = DrugSearchState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    // Cache last tip per session to avoid repeated API calls during testing
    private var _cachedTip: String? = null

    fun generateTip() {
        val patientId = _patientId.value
        if (patientId.isBlank() || _geminiKey.isBlank()) {
            _tipState.value = TipState.Error("Configuration error. Check API key.")
            return
        }
        // Return cached tip immediately if available (avoids 429 during repeated testing)
        _cachedTip?.let {
            _tipState.value = TipState.Success(it)
            return
        }
        viewModelScope.launch {
            _tipState.value = TipState.Loading
            repo.generateTip(patientId, _geminiKey).fold(
                onSuccess = { tip ->
                    _cachedTip = tip
                    _tipState.value = TipState.Success(tip)
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    repo.saveTip(MedCoachTipEntity(patientId = patientId, tipText = tip, timestamp = ts))
                },
                onFailure = { e ->
                    val msg = e.message ?: "Failed to generate tip"
                    val friendlyMsg = when {
                        msg.contains("429") -> "Rate limit reached. Please wait a minute and try again."
                        msg.contains("401") || msg.contains("403") -> "Invalid API key. Check your Gemini API key."
                        msg.contains("network") || msg.contains("connect", ignoreCase = true) -> "Network error. Check your connection."
                        else -> msg
                    }
                    _tipState.value = TipState.Error(friendlyMsg)
                }
            )
        }
    }

    fun clearTipCache() { _cachedTip = null }

    fun resetDrugState() { _drugState.value = DrugSearchState.Idle }
}

// SETTINGS VIEW MODEL
class SettingsViewModel(private val repo: MedTrackRepository) : ViewModel() {

    private val _patient = MutableStateFlow<PatientEntity?>(null)
    val patient: StateFlow<PatientEntity?> = _patient

    fun load(patientId: String) {
        viewModelScope.launch {
            _patient.value = repo.getPatientById(patientId)
        }
    }

    fun logout(onDone: () -> Unit) {
        repo.clearSession()
        onDone()
    }
}

// CLINICIAN VIEW MODEL
data class ClinicianStats(
    val totalPatients: Int,
    val avgMedsPerPatient: Double,
    val mostCommonSymptom: String,
    val avgSymptomSeverity: Double
)

sealed class InsightState {
    object Idle : InsightState()
    object Loading : InsightState()
    data class Success(val insights: List<String>) : InsightState()
    data class Error(val message: String) : InsightState()
}

class ClinicianViewModel(private val repo: MedTrackRepository) : ViewModel() {

    private val _stats = MutableStateFlow<ClinicianStats?>(null)
    val stats: StateFlow<ClinicianStats?> = _stats

    private val _insightState = MutableStateFlow<InsightState>(InsightState.Idle)
    val insightState: StateFlow<InsightState> = _insightState

    private var _geminiKey = ""

    fun load(geminiApiKey: String) {
        _geminiKey = geminiApiKey
        viewModelScope.launch {
            val totalPatients = repo.countPatients()
            val avgMeds = repo.avgMedsPerPatient() ?: 0.0
            val mostCommonSymptom = repo.getMostCommonSymptom() ?: "N/A"
            val avgSeverity = repo.getAvgSymptomSeverity() ?: 0.0
            _stats.value = ClinicianStats(totalPatients, avgMeds, mostCommonSymptom, avgSeverity)
        }
    }

    fun findPatterns() {
        if (_geminiKey.isBlank()) {
            _insightState.value = InsightState.Error("API key not configured")
            return
        }
        viewModelScope.launch {
            _insightState.value = InsightState.Loading
            repo.generateClinicianInsights(_geminiKey).fold(
                onSuccess = { text ->
                    val insights = text.lines()
                        .filter { it.matches(Regex("^\\d+\\..*")) }
                        .map { it.trimStart('1', '2', '3', '.', ' ') }
                        .take(3)
                    _insightState.value = if (insights.isNotEmpty()) InsightState.Success(insights)
                    else InsightState.Success(listOf(text))
                },
                onFailure = { _insightState.value = InsightState.Error(it.message ?: "Failed") }
            )
        }
    }
}
