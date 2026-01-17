package com.umy.medremindid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umy.medremindid.data.local.entity.NotificationPreferenceEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalTime

@Dao
interface NotificationPreferenceDao {

    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): NotificationPreferenceEntity?

    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    fun observeByUserId(userId: Long): Flow<NotificationPreferenceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: NotificationPreferenceEntity): Long

    @Query(
        """
        UPDATE notification_preferences
        SET notificationsEnabled = :enabled,
            updatedAt = :updatedAt
        WHERE userId = :userId
        """
    )
    suspend fun updateNotificationsEnabled(userId: Long, enabled: Boolean, updatedAt: Instant): Int

    @Query(
        """
        UPDATE notification_preferences
        SET quietHoursStart = :start,
            quietHoursEnd = :end,
            updatedAt = :updatedAt
        WHERE userId = :userId
        """
    )
    suspend fun updateQuietHours(userId: Long, start: LocalTime?, end: LocalTime?, updatedAt: Instant): Int

    @Query(
        """
        UPDATE notification_preferences
        SET allowVibration = :allow,
            updatedAt = :updatedAt
        WHERE userId = :userId
        """
    )
    suspend fun updateVibration(userId: Long, allow: Boolean, updatedAt: Instant): Int

    @Query(
        """
        UPDATE notification_preferences
        SET ringtoneUri = :ringtoneUri,
            updatedAt = :updatedAt
        WHERE userId = :userId
        """
    )
    suspend fun updateRingtoneUri(userId: Long, ringtoneUri: String?, updatedAt: Instant): Int
}
