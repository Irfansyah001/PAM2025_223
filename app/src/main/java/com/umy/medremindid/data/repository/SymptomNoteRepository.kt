package com.umy.medremindid.data.repository

import com.umy.medremindid.data.local.dao.SymptomNoteDao
import com.umy.medremindid.data.local.entity.SymptomNoteEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class SymptomNoteRepository(
    private val dao: SymptomNoteDao
) {

    fun observeByUser(userId: Long): Flow<List<SymptomNoteDao.SymptomNoteItem>> =
        dao.observeByUser(userId)

    suspend fun getById(userId: Long, noteId: Long): SymptomNoteEntity? =
        dao.getById(userId, noteId)

    suspend fun upsertForUser(userId: Long, entity: SymptomNoteEntity): Long {
        val now = Instant.now()
        val fixed = if (entity.noteId == 0L) {
            entity.copy(userId = userId, createdAt = now, updatedAt = now)
        } else {
            entity.copy(userId = userId, updatedAt = now)
        }
        return dao.upsert(fixed)
    }

    suspend fun deleteById(userId: Long, noteId: Long) {
        dao.deleteById(userId, noteId)
    }
}
