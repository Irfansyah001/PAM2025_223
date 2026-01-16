package com.umy.medremindid.data.local.dao

import androidx.room.*
import com.umy.medremindid.data.local.entity.MedicationScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationScheduleDao {

    @Query("SELECT * FROM medication_schedules WHERE userId = :userId ORDER BY timeOfDay ASC")
    fun observeByUser(userId: Long): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE scheduleId = :scheduleId LIMIT 1")
    suspend fun getById(scheduleId: Long): MedicationScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: MedicationScheduleEntity): Long

    @Update
    suspend fun update(schedule: MedicationScheduleEntity): Int

    @Delete
    suspend fun delete(schedule: MedicationScheduleEntity): Int

    @Query("SELECT * FROM medication_schedules WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveSchedules(userId: Long): List<MedicationScheduleEntity>
}
