package com.umy.medremindid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.umy.medremindid.data.local.entity.AdherenceLogEntity
import com.umy.medremindid.data.local.entity.AdherenceStatus
import com.umy.medremindid.data.local.model.AdherenceLogWithSchedule
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface AdherenceLogDao {

    @Query("""
        SELECT * FROM adherence_logs
        WHERE userId = :userId
        ORDER BY plannedTime DESC
    """)
    fun observeByUser(userId: Long): Flow<List<AdherenceLogEntity>>

    @Query("""
        SELECT * FROM adherence_logs
        WHERE userId = :userId AND scheduleId = :scheduleId
        ORDER BY plannedTime DESC
    """)
    fun observeBySchedule(userId: Long, scheduleId: Long): Flow<List<AdherenceLogEntity>>

    @Query("""
        SELECT * FROM adherence_logs
        WHERE userId = :userId
          AND plannedTime >= :fromInclusive
          AND plannedTime < :toExclusive
        ORDER BY plannedTime DESC
    """)
    fun observeByPlannedRange(
        userId: Long,
        fromInclusive: Instant,
        toExclusive: Instant
    ): Flow<List<AdherenceLogEntity>>

    // ====== BARU: History + nama obat/dosis ======
    @Query("""
        SELECT l.*, s.medicineName AS medicineName, s.dosage AS dosage
        FROM adherence_logs l
        INNER JOIN medication_schedules s
            ON s.scheduleId = l.scheduleId
        WHERE l.userId = :userId
        ORDER BY l.plannedTime DESC
    """)
    fun observeByUserWithSchedule(userId: Long): Flow<List<AdherenceLogWithSchedule>>

    @Query("""
        SELECT l.*, s.medicineName AS medicineName, s.dosage AS dosage
        FROM adherence_logs l
        INNER JOIN medication_schedules s
            ON s.scheduleId = l.scheduleId
        WHERE l.userId = :userId
          AND l.plannedTime >= :fromInclusive
          AND l.plannedTime < :toExclusive
        ORDER BY l.plannedTime DESC
    """)
    fun observeByPlannedRangeWithSchedule(
        userId: Long,
        fromInclusive: Instant,
        toExclusive: Instant
    ): Flow<List<AdherenceLogWithSchedule>>
    // ============================================

    @Query("""
        SELECT * FROM adherence_logs
        WHERE userId = :userId
          AND scheduleId = :scheduleId
          AND plannedTime = :plannedTime
        LIMIT 1
    """)
    suspend fun getByUnique(userId: Long, scheduleId: Long, plannedTime: Instant): AdherenceLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: AdherenceLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReplace(entity: AdherenceLogEntity): Long

    @Update
    suspend fun update(entity: AdherenceLogEntity)

    @Delete
    suspend fun delete(entity: AdherenceLogEntity)

    @Query("""
        DELETE FROM adherence_logs
        WHERE userId = :userId AND logId = :logId
    """)
    suspend fun deleteById(userId: Long, logId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM medication_schedules
        WHERE userId = :userId AND scheduleId = :scheduleId
    """)
    suspend fun countScheduleOwnedByUser(userId: Long, scheduleId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM adherence_logs
        WHERE userId = :userId AND status = :status
          AND plannedTime >= :fromInclusive AND plannedTime < :toExclusive
    """)
    suspend fun countByStatusInRange(
        userId: Long,
        status: AdherenceStatus,
        fromInclusive: Instant,
        toExclusive: Instant
    ): Int
}
