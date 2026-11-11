package com.driveup.data.local

import androidx.room.*
import com.driveup.data.model.Semester
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY id DESC LIMIT 1")
    fun observeLatest(): Flow<Semester?>

    @Query("SELECT * FROM semesters ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): Semester?

    @Query("SELECT COUNT(*) FROM semesters")
    fun count(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(semester: Semester): Long

    @Query("DELETE FROM semesters")
    suspend fun deleteAll()
}
