package com.umy.medremindid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.umy.medremindid.data.local.entity.NotificationPreferenceEntity

@Dao
interface NotificationPreferenceDao {

    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): NotificationPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: NotificationPreferenceEntity): Long

    @Update
    suspend fun update(pref: NotificationPreferenceEntity): Int
}
