package com.umy.medremindid.data.repository

import com.umy.medremindid.data.local.dao.NotificationPreferenceDao
import com.umy.medremindid.data.local.entity.NotificationPreferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime

class NotificationPreferenceRepository(
    private val dao: NotificationPreferenceDao
) {

    fun observeOrDefault(userId: Long): Flow<NotificationPreferenceEntity> {
        return dao.observeByUserId(userId).map { it ?: defaultPref(userId) }
    }

    suspend fun getOrDefault(userId: Long): NotificationPreferenceEntity = withContext(Dispatchers.IO) {
        dao.getByUserId(userId) ?: defaultPref(userId)
    }

    suspend fun ensureExists(userId: Long): NotificationPreferenceEntity = withContext(Dispatchers.IO) {
        val existing = dao.getByUserId(userId)
        if (existing != null) return@withContext existing

        val now = Instant.now()
        val id = dao.upsert(
            NotificationPreferenceEntity(
                userId = userId,
                createdAt = now,
                updatedAt = now
            )
        )
        NotificationPreferenceEntity(
            preferenceId = id,
            userId = userId,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun save(userId: Long, pref: NotificationPreferenceEntity): Long = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val fixed = if (pref.preferenceId == 0L) {
            pref.copy(userId = userId, createdAt = now, updatedAt = now)
        } else {
            pref.copy(userId = userId, updatedAt = now)
        }
        dao.upsert(fixed)
    }

    suspend fun setNotificationsEnabled(userId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        ensureExists(userId)
        val updated = dao.updateNotificationsEnabled(userId, enabled, Instant.now())
        if (updated == 0) {
            save(userId, defaultPref(userId).copy(notificationsEnabled = enabled))
        }
    }

    suspend fun setQuietHours(userId: Long, start: LocalTime?, end: LocalTime?) = withContext(Dispatchers.IO) {
        ensureExists(userId)

        val fixedStart: LocalTime?
        val fixedEnd: LocalTime?
        if (start == null || end == null) {
            fixedStart = null
            fixedEnd = null
        } else {
            fixedStart = start
            fixedEnd = end
        }

        val updated = dao.updateQuietHours(userId, fixedStart, fixedEnd, Instant.now())
        if (updated == 0) {
            save(userId, defaultPref(userId).copy(quietHoursStart = fixedStart, quietHoursEnd = fixedEnd))
        }
    }

    suspend fun setVibration(userId: Long, allow: Boolean) = withContext(Dispatchers.IO) {
        ensureExists(userId)
        val updated = dao.updateVibration(userId, allow, Instant.now())
        if (updated == 0) {
            save(userId, defaultPref(userId).copy(allowVibration = allow))
        }
    }

    suspend fun setRingtoneUri(userId: Long, ringtoneUri: String?) = withContext(Dispatchers.IO) {
        ensureExists(userId)
        val updated = dao.updateRingtoneUri(userId, ringtoneUri, Instant.now())
        if (updated == 0) {
            save(userId, defaultPref(userId).copy(ringtoneUri = ringtoneUri))
        }
    }

    fun isInQuietHours(pref: NotificationPreferenceEntity, now: LocalTime): Boolean {
        val start = pref.quietHoursStart ?: return false
        val end = pref.quietHoursEnd ?: return false

        return if (start <= end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
    }

    private fun defaultPref(userId: Long): NotificationPreferenceEntity {
        val now = Instant.now()
        return NotificationPreferenceEntity(
            userId = userId,
            notificationsEnabled = true,
            quietHoursStart = null,
            quietHoursEnd = null,
            allowVibration = true,
            ringtoneUri = null,
            createdAt = now,
            updatedAt = now
        )
    }
}
