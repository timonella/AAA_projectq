package com.example.task_manager

import androidx.room.TypeConverter
import java.util.Date
import java.util.Calendar

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // Конвертер для Priority (enum)
    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(priority: String): Priority {
        return Priority.valueOf(priority)
    }

    // Конвертер для RepeatType (enum)
    @TypeConverter
    fun fromRepeatType(repeatType: RepeatType): String {
        return repeatType.name
    }

    @TypeConverter
    fun toRepeatType(repeatType: String): RepeatType {
        return RepeatType.valueOf(repeatType)
    }

    // Конвертер для Calendar (если используется)
    @TypeConverter
    fun fromCalendar(calendar: Calendar?): Long? {
        return calendar?.timeInMillis
    }

    @TypeConverter
    fun toCalendar(timestamp: Long?): Calendar? {
        return timestamp?.let {
            Calendar.getInstance().apply {
                timeInMillis = it
            }
        }
    }
}