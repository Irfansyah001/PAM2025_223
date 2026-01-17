package com.umy.medremindid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface SymptomNoteDao {

    data class SymptomNoteItem(
        val noteId: Long,
        val userId: Long,
        val scheduleId: Long?,
        val title: String,
        val description: String,
        val noteDate: LocalDate,
        val severity: Int?,
        val createdAt: Instant,
        val updatedAt: Instant,
        val medicineName: String?,
        val dosage: String?,
        val timeOfDay: LocalTime?
    )

    @Query(
        """
        SELECT 
          n.noteId AS noteId,
          n.userId AS userId,
          n.scheduleId AS scheduleId,
          n.title AS title,
          n.description AS description,
          n.noteDate AS noteDate,
          n.severity AS severity,
          n.createdAt AS createdAt,
          n.updatedAt AS updatedAt,
          s.medicineName AS medicineName,
          s.dosage AS dosage,
          s.timeOfDay AS timeOfDay
        FROM symptom_notes n
        LEFT JOIN medication_schedules s ON s.scheduleId = n.scheduleId
        WHERE n.userId = :userId
        ORDER BY n.noteDate DESC, n.updatedAt DESC
        """
    )
    fun observeByUser(userId: Long): Flow<List<SymptomNoteItem>>

    @Query(
        """
        SELECT * FROM symptom_notes
        WHERE userId = :userId AND noteId = :noteId
        LIMIT 1
        """
    )
    suspend fun getById(userId: Long, noteId: Long): com.umy.medremindid.data.local.entity.SymptomNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: com.umy.medremindid.data.local.entity.SymptomNoteEntity): Long

    @Update
    suspend fun update(entity: com.umy.medremindid.data.local.entity.SymptomNoteEntity)

    @Delete
    suspend fun delete(entity: com.umy.medremindid.data.local.entity.SymptomNoteEntity)

    @Query(
        """
        DELETE FROM symptom_notes
        WHERE userId = :userId AND noteId = :noteId
        """
    )
    suspend fun deleteById(userId: Long, noteId: Long): Int
}
