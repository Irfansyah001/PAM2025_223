package com.umy.medremindid.data.local.dao

import androidx.room.*
import com.umy.medremindid.data.local.entity.SymptomNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomNoteDao {

    @Query("SELECT * FROM symptom_notes WHERE userId = :userId ORDER BY noteDate DESC, noteId DESC")
    fun observeByUser(userId: Long): Flow<List<SymptomNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: SymptomNoteEntity): Long

    @Update
    suspend fun update(note: SymptomNoteEntity): Int

    @Delete
    suspend fun delete(note: SymptomNoteEntity): Int
}
