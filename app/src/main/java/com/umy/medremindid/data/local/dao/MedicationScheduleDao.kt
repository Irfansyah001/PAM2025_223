package com.umy.medremindid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.umy.medremindid.data.local.entity.MedicationScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalTime
import java.time.LocalDate

@Dao
interface MedicationScheduleDao {

    @Query("""
        SELECT * FROM medication_schedules
        WHERE userId = :userId
        ORDER BY isActive DESC, timeOfDay ASC
    """)
    fun observeAllByUser(userId: Long): Flow<List<MedicationScheduleEntity>>

    @Query("""
        SELECT * FROM medication_schedules
        WHERE userId = :userId AND isActive = 1
        ORDER BY timeOfDay ASC
    """)
    fun observeActiveByUser(userId: Long): Flow<List<MedicationScheduleEntity>>

    @Query("""
    SELECT * FROM medication_schedules
    WHERE isActive = 1
      AND startDate <= :today
      AND (endDate IS NULL OR endDate >= :today)
    ORDER BY timeOfDay ASC
    """)
    suspend fun getAllActive(today: LocalDate = LocalDate.now()): List<MedicationScheduleEntity>

    @Query("""
        SELECT * FROM medication_schedules
        WHERE userId = :userId AND scheduleId = :scheduleId
        LIMIT 1
    """)
    suspend fun getById(userId: Long, scheduleId: Long): MedicationScheduleEntity?

    @Query("""
        SELECT * FROM medication_schedules
        WHERE userId = :userId
          AND (
            medicineName LIKE '%' || :query || '%'
            OR dosage LIKE '%' || :query || '%'
          )
        ORDER BY isActive DESC, timeOfDay ASC
    """)
    fun observeSearch(userId: Long, query: String): Flow<List<MedicationScheduleEntity>>

    @Query("""
        SELECT COUNT(*) FROM medication_schedules
        WHERE userId = :userId
          AND timeOfDay = :timeOfDay
          AND isActive = 1
          AND (:excludeScheduleId IS NULL OR scheduleId != :excludeScheduleId)
    """)
    suspend fun countActiveAtTime(
        userId: Long,
        timeOfDay: LocalTime,
        excludeScheduleId: Long? = null
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MedicationScheduleEntity): Long

    @Update
    suspend fun update(entity: MedicationScheduleEntity)

    @Delete
    suspend fun delete(entity: MedicationScheduleEntity)

    @Query("""
        DELETE FROM medication_schedules
        WHERE userId = :userId AND scheduleId = :scheduleId
    """)
    suspend fun deleteById(userId: Long, scheduleId: Long): Int

    @Query("""
        UPDATE medication_schedules
        SET isActive = :active,
            updatedAt = :updatedAt
        WHERE userId = :userId AND scheduleId = :scheduleId
    """)
    suspend fun setActive(
        userId: Long,
        scheduleId: Long,
        active: Boolean,
        updatedAt: Instant
    ): Int
}
