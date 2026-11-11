package com.driveup.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    indices = [Index(value = ["semesterId"])]
)
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long,
    val name: String,
    val location: String,
    val teacher: String = "",
    val dayOfWeek: Int,        // 1=Mon … 7=Sun
    val startSection: Int,     // 1–11
    val endSection: Int,       // >= startSection
    val weeks: String,         // compact: "1-13,15-16"
    val weekPattern: Int,      // 0=all/mixed, 1=odd, 2=even
    val colorIndex: Int = 0
)
