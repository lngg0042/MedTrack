package com.ng.s33986010.medtrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ng.s33986010.medtrack.data.dao.*
import com.ng.s33986010.medtrack.model.*

@Database(
    entities = [
        PatientEntity::class,
        MedicationEntity::class,
        SymptomEntity::class,
        MedCoachTipEntity::class,
        TakenStatusEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MedTrackDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun medicationDao(): MedicationDao
    abstract fun symptomDao(): SymptomDao
    abstract fun medCoachTipDao(): MedCoachTipDao
    abstract fun takenStatusDao(): TakenStatusDao

    companion object {
        @Volatile private var INSTANCE: MedTrackDatabase? = null

        fun getInstance(context: Context): MedTrackDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedTrackDatabase::class.java,
                    "medtrack_db"
                ).build().also { INSTANCE = it }
            }
    }
}
