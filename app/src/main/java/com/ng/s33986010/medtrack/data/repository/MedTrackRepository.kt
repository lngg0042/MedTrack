package com.ng.s33986010.medtrack.data.repository

import android.content.Context
import android.util.Log
import com.ng.s33986010.medtrack.data.dao.*
import com.ng.s33986010.medtrack.data.database.MedTrackDatabase
import com.ng.s33986010.medtrack.model.*
import com.ng.s33986010.medtrack.network.*
import kotlinx.coroutines.flow.Flow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class MedTrackRepository(private val context: Context) {

    private val db = MedTrackDatabase.getInstance(context)
    private val patientDao: PatientDao = db.patientDao()
    private val medicationDao: MedicationDao = db.medicationDao()
    private val symptomDao: SymptomDao = db.symptomDao()
    private val tipDao: MedCoachTipDao = db.medCoachTipDao()
    private val takenDao: TakenStatusDao = db.takenStatusDao()

    private val prefs = context.getSharedPreferences("medtrack_prefs", Context.MODE_PRIVATE)

    // Session
    fun getLoggedInPatientId(): String? = prefs.getString("logged_in_patient_id", null)
    fun saveSession(patientId: String) = prefs.edit().putString("logged_in_patient_id", patientId).apply()
    fun clearSession() = prefs.edit().remove("logged_in_patient_id").apply()

    // DB Seeding
    fun isDbSeeded(): Boolean = prefs.getBoolean("db_seeded", false)
    fun markDbSeeded() = prefs.edit().putBoolean("db_seeded", true).apply()

    suspend fun seedFromCsv() {
        seedPatients()
        seedMedications()
        seedSymptoms()
        markDbSeeded()
    }

    private suspend fun seedPatients() {
        val patients = mutableListOf<PatientEntity>()
        try {
            context.assets.open("patients.csv").bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val v = line.trim().split(",")
                    if (v.size >= 3) {
                        patients.add(PatientEntity(
                            patientId = v[0].trim(),
                            phoneNumber = v[1].trim(),
                            name = v[2].trim(),
                            password = null  // claimed later
                        ))
                    }
                }
            }
        } catch (e: Exception) { Log.e("Repo", "seedPatients error", e) }
        patientDao.insertAll(patients)
    }

    private suspend fun seedMedications() {
        val meds = mutableListOf<MedicationEntity>()
        try {
            context.assets.open("medications.csv").bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val v = line.trim().split(",")
                    if (v.size >= 5) {
                        meds.add(MedicationEntity(
                            patientId = v[0].trim(),
                            medicationName = v[1].trim(),
                            dosage = v[2].trim(),
                            frequency = v[3].trim(),
                            scheduledTime = v[4].trim(),
                            medicationType = if (v.size >= 6) v[5].trim() else "",
                            notes = if (v.size >= 7) v[6].trim() else ""
                        ))
                    }
                }
            }
        } catch (e: Exception) { Log.e("Repo", "seedMedications error", e) }
        medicationDao.insertAll(meds)
    }

    private suspend fun seedSymptoms() {
        val symptoms = mutableListOf<SymptomEntity>()
        try {
            context.assets.open("symptoms.csv").bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val v = line.trim().split(Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)"))
                    if (v.size >= 5) {
                        symptoms.add(SymptomEntity(
                            patientId = v[0].trim(),
                            category = v[1].trim(),
                            severity = v[2].trim().toIntOrNull() ?: 1,
                            notes = v[3].trim().removeSurrounding("\""),
                            dateTime = v[4].trim()
                        ))
                    }
                }
            }
        } catch (e: Exception) { Log.e("Repo", "seedSymptoms error", e) }
        symptomDao.insertAll(symptoms)
    }

    // Patient
    suspend fun getPatientById(id: String) = patientDao.getById(id)
    suspend fun getPatientByIdAndPhone(id: String, phone: String) = patientDao.getByIdAndPhone(id, phone)
    suspend fun getPatientByIdAndPassword(id: String, password: String) = patientDao.getByIdAndPassword(id, password)
    suspend fun updatePatient(patient: PatientEntity) = patientDao.update(patient)
    suspend fun countPatients() = patientDao.countAll()
    suspend fun getAllPatients() = patientDao.getAll()

    suspend fun insertNewPatient(patient: PatientEntity): String {
        patientDao.insert(patient)
        return patient.patientId
    }

    suspend fun generateNewPatientId(): String {
        val all = patientDao.getAll()
        val max = all.mapNotNull { it.patientId.removePrefix("P").toIntOrNull() }.maxOrNull() ?: 1000
        return "P${max + 1}"
    }

    // Medications
    fun getMedicationsFlow(patientId: String): Flow<List<MedicationEntity>> =
        medicationDao.getByPatientFlow(patientId)

    suspend fun getMedications(patientId: String) = medicationDao.getByPatient(patientId)

    suspend fun addMedication(med: MedicationEntity): Long = medicationDao.insert(med)

    suspend fun avgMedsPerPatient() = medicationDao.avgMedsPerPatient()

    // Symptoms
    fun getSymptomsFlow(patientId: String): Flow<List<SymptomEntity>> =
        symptomDao.getByPatientFlow(patientId)

    suspend fun addSymptom(symptom: SymptomEntity) = symptomDao.insert(symptom)

    suspend fun getMostCommonSymptom() = symptomDao.getMostCommonCategory()

    suspend fun getAvgSymptomSeverity() = symptomDao.getAvgSeverity()

    suspend fun getAllSymptoms() = symptomDao.getAll()

    // Band I: Symptom Trend
    suspend fun getSymptomsForTrend(patientId: String): List<SymptomEntity> =
        symptomDao.getByPatient(patientId)

    // MedCoach Tips
    fun getTipsFlow(patientId: String): Flow<List<MedCoachTipEntity>> =
        tipDao.getByPatientFlow(patientId)

    suspend fun saveTip(tip: MedCoachTipEntity) = tipDao.insert(tip)

    // Taken Status (Band G)
    suspend fun getTakenStatusForMeds(medIds: List<Int>): Map<Int, Boolean> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        takenDao.clearOldEntries(today)
        val statuses = takenDao.getForMeds(medIds)
        return statuses.associate { it.medicationId to it.isTaken }
    }

    suspend fun setTakenStatus(medicationId: Int, taken: Boolean) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        takenDao.insertOrReplace(TakenStatusEntity(medicationId, taken, today))
    }

    // OpenFDA API
    suspend fun searchDrug(name: String): Result<DrugLabelResult> {
        return try {
            val response = RetrofitClient.fdaApi.searchDrug(
                search = "openfda.brand_name:\"$name\"+openfda.generic_name:\"$name\"",
                limit = 1
            )
            if (response.results.isNotEmpty()) {
                Result.success(response.results[0])
            } else {
                // fallback: generic name only
                val resp2 = RetrofitClient.fdaApi.searchDrug(
                    search = "openfda.generic_name:\"$name\"",
                    limit = 1
                )
                if (resp2.results.isNotEmpty()) Result.success(resp2.results[0])
                else Result.failure(Exception("Drug not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gemini GenAI
    suspend fun generateTip(patientId: String, apiKey: String): Result<String> {
        return try {
            val patient = patientDao.getById(patientId)
            val meds = medicationDao.getByPatient(patientId)
            val symptoms = symptomDao.getByPatient(patientId).takeLast(5)

            val medNames = meds.joinToString(", ") { it.medicationName }
            val symptomSummary = symptoms.joinToString("; ") { "${it.category} (severity ${it.severity})" }

            val prompt = buildString {
                append("You are a health assistant. Generate a short, warm, personalised medication adherence tip (2-3 sentences) for a patient named ${patient?.name ?: "the patient"}. ")
                if (medNames.isNotEmpty()) append("They are currently taking: $medNames. ")
                if (symptomSummary.isNotEmpty()) append("Their recent symptoms include: $symptomSummary. ")
                append("Make it encouraging and specific to their medications. Keep it under 100 words.")
            }

            val response = RetrofitClient.geminiApi.generateContent(
                apiKey = apiKey,
                body = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
                )
            )
            val text = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateClinicianInsights(apiKey: String): Result<String> {
        return try {
            val patients = patientDao.getAll()
            val meds = medicationDao.getAll()
            val symptoms = symptomDao.getAll()

            val prompt = buildString {
                append("You are a medical data analyst. Here is anonymised aggregate patient data:\n")
                append("- Total patients: ${patients.size}\n")
                append("- Total medications: ${meds.size}\n")
                append("- Total symptoms logged: ${symptoms.size}\n")
                append("- Symptom category breakdown: ${symptoms.groupBy { it.category }.map { "${it.key}: ${it.value.size}" }.joinToString(", ")}\n")
                append("- Average symptom severity: ${"%.1f".format(symptoms.map { it.severity }.average())}\n")
                append("- Medication frequency breakdown: ${meds.groupBy { it.frequency }.map { "${it.key}: ${it.value.size}" }.joinToString(", ")}\n\n")
                append("Based on this data, provide exactly 3 interesting, data-driven clinical observations or patterns. ")
                append("Format as a numbered list (1. 2. 3.). Be specific and insightful.")
            }

            val response = RetrofitClient.geminiApi.generateContent(
                apiKey = apiKey,
                body = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
                )
            )
            val text = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
