package com.umy.medremindid.data.repository

import com.umy.medremindid.data.local.dao.MedicationScheduleDao
import com.umy.medremindid.data.local.entity.MedicationScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalTime

class MedicationScheduleRepository(
    private val dao: MedicationScheduleDao
) {

    fun observeAllByUser(userId: Long): Flow<List<MedicationScheduleEntity>> =
        dao.observeAllByUser(userId)

    fun observeActiveByUser(userId: Long): Flow<List<MedicationScheduleEntity>> =
        dao.observeActiveByUser(userId)

    fun observeSearch(userId: Long, query: String): Flow<List<MedicationScheduleEntity>> =
        dao.observeSearch(userId, query.trim())

    suspend fun getById(userId: Long, scheduleId: Long): MedicationScheduleEntity? =
        dao.getById(userId, scheduleId)

    suspend fun countActiveAtTime(
        userId: Long,
        timeOfDay: LocalTime,
        excludeScheduleId: Long? = null
    ): Int {
        return dao.countActiveAtTime(userId, timeOfDay, excludeScheduleId)
    }

    suspend fun upsert(entity: MedicationScheduleEntity): Long {
        val now = Instant.now()

        val fixed = if (entity.scheduleId == 0L) {
            entity.copy(
                createdAt = now,
                updatedAt = now
            )
        } else {
            val existing = dao.getById(entity.userId, entity.scheduleId)
            entity.copy(
                createdAt = existing?.createdAt ?: entity.createdAt,
                updatedAt = now
            )
        }

        return dao.upsert(fixed)
    }

    suspend fun deleteById(userId: Long, scheduleId: Long): Int =
        dao.deleteById(userId, scheduleId)

    suspend fun setActive(userId: Long, scheduleId: Long, active: Boolean): Int {
        return dao.setActive(
            userId = userId,
            scheduleId = scheduleId,
            active = active,
            updatedAt = Instant.now()
        )
    }
}
