package com.umy.medremindid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umy.medremindid.data.local.converter.AdherenceStatus
import com.umy.medremindid.data.local.entity.AdherenceLogEntity
import kotlinx.coroutines.flow.Flow

data class StatusCount(
    val status: AdherenceStatus,
    val count: Int
)

@Dao
interface AdherenceLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: AdherenceLogEntity): Long

    @Query("SELECT * FROM adherence_logs WHERE userId = :userId ORDER BY plannedTime DESC")
    fun observeByUser(userId: Long): Flow<List<AdherenceLogEntity>>

    @Query("""
        SELECT status as status, COUNT(*) as count
        FROM adherence_logs
        WHERE userId = :userId AND plannedTime BETWEEN :fromMillis AND :toMillis
        GROUP BY status
    """)
    suspend fun countByStatusInRange(
        userId: Long,
        fromMillis: Long,
        toMillis: Long
    ): List<StatusCount>
}
