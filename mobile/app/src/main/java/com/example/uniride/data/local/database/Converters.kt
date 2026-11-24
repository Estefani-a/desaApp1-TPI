package com.example.uniride.data.local.database

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Room type converters. Enums need none: Room stores them by name.
 *
 * - [LocalDate] and [LocalTime] are stored as ISO text (`2026-11-20`, `22:15:00`). The format
 *   has a fixed width, so sorting and comparing the text is the same as sorting and comparing
 *   the dates and times, which the feed queries rely on.
 * - [Instant] is stored as epoch milliseconds (`Long`): it is compact and sorts correctly.
 *   The text form of an instant has a variable length (`...:00Z` vs `...:00.5Z`) and would not.
 * - `List<String>` is stored as a JSON array written by Moshi, not joined with a separator:
 *   a meeting point could contain any separator character.
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.format(DateTimeFormatter.ISO_LOCAL_TIME)

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromStringList(value: List<String>): String = stringListAdapter.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = stringListAdapter.fromJson(value).orEmpty()

    private companion object {
        val stringListAdapter: JsonAdapter<List<String>> = Moshi.Builder().build()
            .adapter(Types.newParameterizedType(List::class.java, String::class.java))
    }
}
