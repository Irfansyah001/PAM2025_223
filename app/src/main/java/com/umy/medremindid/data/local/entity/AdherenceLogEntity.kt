package com.umy.medremindid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "adherence_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MedicationScheduleEntity::class,
            parentColumns = ["scheduleId"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("scheduleId"),
        Index(value = ["userId", "plannedTime"]),
        // 1 log untuk 1 jadwal pada 1 plannedTime (idempotent saat nanti WorkManager menandai MISSED)
        Index(value = ["userId", "scheduleId", "plannedTime"], unique = true)
    ]
)
data class AdherenceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0L,

    val userId: Long,
    val scheduleId: Long,

    val plannedTime: Instant,
    val takenTime: Instant? = null,

    val status: AdherenceStatus,
    val note: String? = null,

    val createdAt: Instant = Instant.now()
)
