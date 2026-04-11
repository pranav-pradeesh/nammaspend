package com.nammaspendai.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.format(formatter)
    }
}

class LocalDateConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, formatter) }
    }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? {
        return date?.format(formatter)
    }
}

class MapConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMap(value: Map<Int, Int>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toMap(value: String?): Map<Int, Int>? {
        return value?.let {
            val type = object : TypeToken<Map<Int, Int>>() {}.type
            gson.fromJson<Map<Int, Int>>(it, type)
        }
    }
}

class IntMapConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromIntMap(value: MutableMap<Int, Int>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toIntMap(value: String?): MutableMap<Int, Int>? {
        return value?.let {
            val type = object : TypeToken<MutableMap<Int, Int>>() {}.type
            gson.fromJson<MutableMap<Int, Int>>(it, type)
        }
    }
}
