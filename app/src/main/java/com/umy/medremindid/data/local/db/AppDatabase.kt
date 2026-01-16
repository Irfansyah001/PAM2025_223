package com.umy.medremindid.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.umy.medremindid.data.local.converter.Converters
import com.umy.medremindid.data.local.dao.*
import com.umy.medremindid.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        NotificationPreferenceEntity::class,
        MedicationScheduleEntity::class,
        AdherenceLogEntity::class,
        SymptomNoteEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun notificationPreferenceDao(): NotificationPreferenceDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun adherenceLogDao(): AdherenceLogDao
    abstract fun symptomNoteDao(): SymptomNoteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medremindid.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
