package com.driveup.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveup.data.model.Course
import com.driveup.data.model.Semester
import com.driveup.data.parser.ExcelParser
import com.driveup.data.repository.TimetableRepository
import com.driveup.utils.WeekCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val semester: Semester? = null,
    val currentWeek: Int = 1,
    val displayWeek: Int = 1,
    val totalWeeks: Int = 20,
    val weekLabel: String = "",
    val isCurrentWeek: Boolean = true,
    val todayDayOfWeek: Int = 0,          // 1–7, 0 if not current week
    val coursesThisWeek: List<Course> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: TimetableRepository) : ViewModel() {

    private val parser = ExcelParser()

    private val _displayWeek = MutableStateFlow(1)
    val uiState: StateFlow<MainUiState> = repository.observeSemester()
        .filterNotNull()
        .flatMapLatest { semester ->
            val startDate = WeekCalculator.parseDate(semester.startDate)
            val realWeek = WeekCalculator.currentWeek(startDate).coerceIn(1, semester.totalWeeks)
            _displayWeek.value = realWeek

            combine(
                repository.observeCourses(semester.id),
                _displayWeek
            ) { allCourses: List<Course>, displayWeek: Int ->
                val weekStart = WeekCalculator.weekStartDate(startDate, displayWeek)
                val weekLabel = WeekCalculator.formatWeekLabel(displayWeek, weekStart)
                val isCurrentWeek = WeekCalculator.isCurrentWeek(startDate, displayWeek)
                val todayCol = if (isCurrentWeek) WeekCalculator.todayDayOfWeek() else 0

                val weekCourses = allCourses.filter { course: Course ->
                    parser.expandWeeks(course.weeks, course.weekPattern).contains(displayWeek)
                }

                MainUiState(
                    semester = semester,
                    currentWeek = realWeek,
                    displayWeek = displayWeek,
                    totalWeeks = semester.totalWeeks,
                    weekLabel = weekLabel,
                    isCurrentWeek = isCurrentWeek,
                    todayDayOfWeek = todayCol,
                    coursesThisWeek = weekCourses
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    fun previousWeek() {
        _displayWeek.update { it.coerceAtLeast(2) - 1 }
    }

    fun nextWeek() {
        val total = uiState.value.totalWeeks
        _displayWeek.update { (it + 1).coerceAtMost(total) }
    }

    fun jumpToCurrentWeek() {
        _displayWeek.value = uiState.value.currentWeek
    }

    fun updateStartDate(newStartDate: String) {
        val semester = uiState.value.semester ?: return
        viewModelScope.launch {
            repository.updateStartDate(semester, newStartDate)
        }
    }
}
