package com.driveup.data.repository

import android.content.Context
import android.net.Uri
import com.driveup.data.local.AppDatabase
import com.driveup.data.model.Course
import com.driveup.data.model.Semester
import com.driveup.data.parser.ExcelParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimetableRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val semesterDao = db.semesterDao()
    private val courseDao = db.courseDao()
    private val parser = ExcelParser()

    fun observeSemester(): Flow<Semester?> = semesterDao.observeLatest()

    fun hasSemester(): Flow<Boolean> = semesterDao.count().map { it > 0 }

    suspend fun getSemesterOnce(): Semester? = semesterDao.getLatest()

    suspend fun getCoursesOnce(semesterId: Long): List<Course> =
        courseDao.getAllBySemester(semesterId)

    suspend fun updateStartDate(semester: Semester, newStartDate: String) {
        semesterDao.insert(semester.copy(startDate = newStartDate))
    }

    fun observeCourses(semesterId: Long): Flow<List<Course>> =
        courseDao.observeBySemester(semesterId)

    suspend fun importFromUri(context: Context, uri: Uri, startDate: String): Result<Semester> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法读取文件"))

            val parsed = parser.parse(inputStream)
            inputStream.close()

            if (parsed.courses.isEmpty()) {
                return Result.failure(Exception("未解析到任何课程，请检查文件格式"))
            }

            // Clear existing data
            semesterDao.deleteAll()
            courseDao.deleteAll()

            val semesterId = semesterDao.insert(
                Semester(
                    name = parsed.semesterName,
                    startDate = startDate,
                    totalWeeks = parsed.courses.maxOf { it.maxWeek }
                )
            )

            val courses = parsed.courses.map { it.toCourse(semesterId) }
            courseDao.insertAll(courses)

            val semester = Semester(
                id = semesterId,
                name = parsed.semesterName,
                startDate = startDate,
                totalWeeks = parsed.courses.maxOf { it.maxWeek }
            )
            Result.success(semester)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
