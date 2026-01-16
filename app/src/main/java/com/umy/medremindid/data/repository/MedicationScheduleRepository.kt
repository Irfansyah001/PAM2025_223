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

    /**
     * UPSERT yang aman untuk SRS:
     * - Selalu paksa userId sesuai sesi (ownership tidak boleh kebalik).
     * - createdAt hanya di-set ulang saat insert baru (scheduleId == 0).
     * - updatedAt selalu di-set sekarang.
     */
    suspend fun upsertForUser(userId: Long, entity: MedicationScheduleEntity) {
        val now = Instant.now()

        val fixed = if (entity.scheduleId == 0L) {
            entity.copy(
                userId = userId,
                createdAt = now,
                updatedAt = now
            )
        } else {
            entity.copy(
                userId = userId,
                // createdAt dipertahankan dari entity yang dipass dari UI/edit
                updatedAt = now
            )
        }

        dao.upsert(fixed)
    }

    suspend fun deleteById(userId: Long, scheduleId: Long) {
        dao.deleteById(userId, scheduleId)
    }

    suspend fun setActive(userId: Long, scheduleId: Long, active: Boolean) {
        dao.setActive(
            userId = userId,
            scheduleId = scheduleId,
            active = active,
            updatedAt = Instant.now()
        )
    }

    /**
     * Opsional tapi relevan untuk validasi jadwal sederhana:
     * apakah ada jadwal aktif di timeOfDay yang sama.
     */
    suspend fun hasActiveAtTime(
        userId: Long,
        timeOfDay: LocalTime,
        excludeScheduleId: Long? = null
    ): Boolean {
        return dao.countActiveAtTime(userId, timeOfDay, excludeScheduleId) > 0
    }
}
