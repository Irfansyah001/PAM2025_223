package com.umy.medremindid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "medication_schedules",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index(value = ["userId", "timeOfDay"])
    ]
)
data class MedicationScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val scheduleId: Long = 0L,
    val userId: Long,
    val medicineName: String,
    val dosage: String,
    val instructions: String? = null,
    val timeOfDay: LocalTime,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
