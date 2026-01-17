package com.umy.medremindid.data.repository

import com.umy.medremindid.data.local.dao.AdherenceLogDao
import com.umy.medremindid.data.local.entity.AdherenceLogEntity
import com.umy.medremindid.data.local.entity.AdherenceStatus
import com.umy.medremindid.data.local.model.AdherenceLogWithSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

class AdherenceLogRepository(
    private val dao: AdherenceLogDao
) {
    fun observeByUser(userId: Long): Flow<List<AdherenceLogEntity>> =
        dao.observeByUser(userId)

    fun observeBySchedule(userId: Long, scheduleId: Long): Flow<List<AdherenceLogEntity>> =
        dao.observeBySchedule(userId, scheduleId)

    fun observeByPlannedRange(
        userId: Long,
        fromInclusive: Instant,
        toExclusive: Instant
    ): Flow<List<AdherenceLogEntity>> =
        dao.observeByPlannedRange(userId, fromInclusive, toExclusive)

    // ===== BARU =====
    fun observeByUserWithSchedule(userId: Long): Flow<List<AdherenceLogWithSchedule>> =
        dao.observeByUserWithSchedule(userId)

    fun observeByPlannedRangeWithSchedule(
        userId: Long,
        fromInclusive: Instant,
        toExclusive: Instant
    ): Flow<List<AdherenceLogWithSchedule>> =
        dao.observeByPlannedRangeWithSchedule(userId, fromInclusive, toExclusive)
    // ================

    suspend fun getByUnique(
        userId: Long,
        scheduleId: Long,
        plannedTime: Instant
    ): AdherenceLogEntity? = withContext(Dispatchers.IO) {
        dao.getByUnique(userId, scheduleId, plannedTime)
    }

    private suspend fun ensureScheduleOwned(userId: Long, scheduleId: Long) {
        val ok = withContext(Dispatchers.IO) {
            dao.countScheduleOwnedByUser(userId, scheduleId) > 0
        }
        require(ok) { "Schedule $scheduleId tidak ditemukan untuk userId=$userId" }
    }

    private suspend fun buildUpsertEntity(
        userId: Long,
        scheduleId: Long,
        plannedTime: Instant,
        status: AdherenceStatus,
        takenTime: Instant?,
        note: String?
    ): AdherenceLogEntity {
        val existing = dao.getByUnique(userId, scheduleId, plannedTime)
        val now = Instant.now()

        return if (existing == null) {
            AdherenceLogEntity(
                userId = userId,
                scheduleId = scheduleId,
                plannedTime = plannedTime,
                takenTime = takenTime,
                status = status,
                note = note,
                createdAt = now
            )
        } else {
            existing.copy(
                takenTime = takenTime,
                status = status,
                note = note
            )
        }
    }

    suspend fun recordTaken(
        userId: Long,
        scheduleId: Long,
        plannedTime: Instant,
        takenTime: Instant = Instant.now(),
        note: String? = null
    ) = withContext(Dispatchers.IO) {
        ensureScheduleOwned(userId, scheduleId)

        val entity = buildUpsertEntity(
            userId = userId,
            scheduleId = scheduleId,
            plannedTime = plannedTime,
            status = AdherenceStatus.TAKEN,
            takenTime = takenTime,
            note = note
        )

        dao.upsertReplace(entity)
    }

    suspend fun recordSkipped(
        userId: Long,
        scheduleId: Long,
        plannedTime: Instant,
        note: String? = null
    ) = withContext(Dispatchers.IO) {
        ensureScheduleOwned(userId, scheduleId)

        val entity = buildUpsertEntity(
            userId = userId,
            scheduleId = scheduleId,
            plannedTime = plannedTime,
            status = AdherenceStatus.SKIPPED,
            takenTime = null,
            note = note
        )

        dao.upsertReplace(entity)
    }

    suspend fun recordMissed(
        userId: Long,
        scheduleId: Long,
        plannedTime: Instant,
        note: String? = null
    ) = withContext(Dispatchers.IO) {
        ensureScheduleOwned(userId, scheduleId)

        val entity = buildUpsertEntity(
            userId = userId,
            scheduleId = scheduleId,
            plannedTime = plannedTime,
            status = AdherenceStatus.MISSED,
            takenTime = null,
            note = note
        )

        dao.upsertReplace(entity)
    }

    suspend fun markMissedIfAbsent(
        userId: Long,
        scheduleId: Long,
        plannedTime: Instant,
        note: String? = null
    ) = withContext(Dispatchers.IO) {
        ensureScheduleOwned(userId, scheduleId)

        dao.insertIfAbsent(
            AdherenceLogEntity(
                userId = userId,
                scheduleId = scheduleId,
                plannedTime = plannedTime,
                takenTime = null,
                status = AdherenceStatus.MISSED,
                note = note,
                createdAt = Instant.now()
            )
        )
    }

    suspend fun markTaken(
        userId: Long,
        scheduleId: Long,
        plannedTime: Instant,
        note: String? = null
    ) = recordTaken(userId, scheduleId, plannedTime, Instant.now(), note)

    suspend fun markSkipped(
        userId: Long,
        scheduleId: Long,
        plannedTime: Instant,
        note: String? = null
    ) = recordSkipped(userId, scheduleId, plannedTime, note)

    suspend fun deleteById(userId: Long, logId: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(userId, logId)
    }

    suspend fun countByStatusInRange(
        userId: Long,
        status: AdherenceStatus,
        fromInclusive: Instant,
        toExclusive: Instant
    ): Int = withContext(Dispatchers.IO) {
        dao.countByStatusInRange(userId, status, fromInclusive, toExclusive)
    }
}
