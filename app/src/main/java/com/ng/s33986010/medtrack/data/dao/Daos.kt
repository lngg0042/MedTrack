package com.ng.s33986010.medtrack.data.dao

import androidx.room.*
import com.ng.s33986010.medtrack.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(patients: List<PatientEntity>)

    @Update
    suspend fun update(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE patientId = :id")
    suspend fun getById(id: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE phoneNumber = :phone")
    suspend fun getByPhone(phone: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE patientId = :id AND phoneNumber = :phone")
    suspend fun getByIdAndPhone(id: String, phone: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE patientId = :id AND password = :password")
    suspend fun getByIdAndPassword(id: String, password: String): PatientEntity?

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun countAll(): Int

    @Query("SELECT * FROM patients")
    fun getAllFlow(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients")
    suspend fun getAll(): List<PatientEntity>
}

@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(med: MedicationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(meds: List<MedicationEntity>)

    @Query("SELECT * FROM medications WHERE patientId = :patientId ORDER BY scheduledTime ASC, medicationName ASC")
    fun getByPatientFlow(patientId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE patientId = :patientId ORDER BY scheduledTime ASC, medicationName ASC")
    suspend fun getByPatient(patientId: String): List<MedicationEntity>

    @Query("SELECT COUNT(*) FROM medications WHERE patientId = :patientId")
    suspend fun countForPatient(patientId: String): Int

    @Query("SELECT AVG(cnt) FROM (SELECT COUNT(*) as cnt FROM medications GROUP BY patientId)")
    suspend fun avgMedsPerPatient(): Double?

    @Query("SELECT * FROM medications")
    suspend fun getAll(): List<MedicationEntity>
}

@Dao
interface SymptomDao {
    @Insert
    suspend fun insert(symptom: SymptomEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(symptoms: List<SymptomEntity>)

    @Query("SELECT * FROM symptoms WHERE patientId = :patientId ORDER BY dateTime DESC")
    fun getByPatientFlow(patientId: String): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms WHERE patientId = :patientId ORDER BY dateTime DESC")
    suspend fun getByPatient(patientId: String): List<SymptomEntity>

    @Query("SELECT category FROM symptoms GROUP BY category ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getMostCommonCategory(): String?

    @Query("SELECT AVG(severity) FROM symptoms")
    suspend fun getAvgSeverity(): Double?

    @Query("SELECT * FROM symptoms")
    suspend fun getAll(): List<SymptomEntity>
}

@Dao
interface MedCoachTipDao {
    @Insert
    suspend fun insert(tip: MedCoachTipEntity)

    @Query("SELECT * FROM medcoach_tips WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getByPatientFlow(patientId: String): Flow<List<MedCoachTipEntity>>

    @Query("SELECT * FROM medcoach_tips WHERE patientId = :patientId ORDER BY timestamp DESC")
    suspend fun getByPatient(patientId: String): List<MedCoachTipEntity>
}

@Dao
interface TakenStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(status: TakenStatusEntity)

    @Query("SELECT * FROM taken_status WHERE medicationId = :medId")
    suspend fun getForMed(medId: Int): TakenStatusEntity?

    @Query("SELECT * FROM taken_status WHERE medicationId IN (:medIds)")
    suspend fun getForMeds(medIds: List<Int>): List<TakenStatusEntity>

    @Query("DELETE FROM taken_status WHERE date != :today")
    suspend fun clearOldEntries(today: String)
}
