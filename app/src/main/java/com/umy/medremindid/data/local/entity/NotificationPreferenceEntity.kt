package com.umy.medremindid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalTime

@Entity(
    tableName = "notification_preferences",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId", unique = true)]
)
data class NotificationPreferenceEntity(
    @PrimaryKey(autoGenerate = true)
    val preferenceId: Long = 0L,
    val userId: Long,
    val notificationsEnabled: Boolean = true,
    val quietHoursStart: LocalTime? = null,
    val quietHoursEnd: LocalTime? = null,
    val allowVibration: Boolean = true,
    val ringtoneUri: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
