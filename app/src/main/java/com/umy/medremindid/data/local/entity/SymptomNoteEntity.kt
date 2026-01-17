package com.umy.medremindid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "symptom_notes",
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
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("userId"),
        Index("scheduleId"),
        Index(value = ["userId", "noteDate"])
    ]
)
data class SymptomNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val noteId: Long = 0L,
    val userId: Long,
    val scheduleId: Long? = null,
    val title: String,
    val description: String,
    val noteDate: LocalDate,
    val severity: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
