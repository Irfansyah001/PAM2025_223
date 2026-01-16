package com.umy.medremindid.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0L,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val dateOfBirth: LocalDate?,
    val gender: String?,
    val createdAt: Instant = Instant.now()
)
