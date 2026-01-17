package com.umy.medremindid.data.local.converter

import androidx.room.TypeConverter
import com.umy.medremindid.data.local.entity.AdherenceStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun adherenceStatusToString(value: AdherenceStatus?): String? = value?.name

    @TypeConverter
    fun stringToAdherenceStatus(value: String?): AdherenceStatus? =
        value?.let(AdherenceStatus::valueOf)
}
