package com.umy.medremindid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.umy.medremindid.data.local.converter.AdherenceStatus
import java.time.Instant

@Entity(
    tableName = "adherence_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationScheduleEntity::class,
            parentColumns = ["scheduleId"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("scheduleId"),
        Index("userId"),
        Index(value = ["userId", "plannedTime"])
    ]
)
data class AdherenceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0L,
    val scheduleId: Long,
    val userId: Long,
    val plannedTime: Instant,
    val takenTime: Instant? = null,
    val status: AdherenceStatus,
    val note: String? = null,
    val createdAt: Instant = Instant.now()
)
