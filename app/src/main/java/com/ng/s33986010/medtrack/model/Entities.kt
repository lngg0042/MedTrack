package com.ng.s33986010.medtrack.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val patientId: String,
    val phoneNumber: String,
    val name: String,
    val password: String? = null  // null until account is claimed
)

@Entity(
    tableName = "medications",
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["patientId"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("patientId")]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val scheduledTime: String,
    val medicationType: String,
    val notes: String
)

@Entity(
    tableName = "symptoms",
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["patientId"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("patientId")]
)
data class SymptomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val category: String,
    val severity: Int,
    val notes: String,
    val dateTime: String
)

@Entity(
    tableName = "medcoach_tips",
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["patientId"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("patientId")]
)
data class MedCoachTipEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val tipText: String,
    val timestamp: String
)

// For Band G: Taken toggle persistence
@Entity(
    tableName = "taken_status",
    foreignKeys = [ForeignKey(
        entity = MedicationEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicationId")]
)
data class TakenStatusEntity(
    @PrimaryKey val medicationId: Int,
    val isTaken: Boolean,
    val date: String  // "yyyy-MM-dd" — resets each new day
)
