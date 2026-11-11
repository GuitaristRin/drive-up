package com.driveup.ui.import_

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveup.data.repository.TimetableRepository
import com.driveup.utils.WeekCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val semesterName: String) : ImportState()
    data class Error(val message: String) : ImportState()
}

class ImportViewModel(private val repository: TimetableRepository) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    // Selected start date (default: most recent Monday)
    private val _startDate = MutableStateFlow(mostRecentMonday())
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()

    fun setStartDate(date: LocalDate) {
        _startDate.value = date
    }

    fun importFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = ImportState.Loading
            val dateStr = WeekCalculator.formatDate(_startDate.value)
            val result = repository.importFromUri(context, uri, dateStr)
            _state.value = result.fold(
                onSuccess = { ImportState.Success(it.name) },
                onFailure = { ImportState.Error(it.message ?: "未知错误") }
            )
        }
    }

    fun resetError() {
        _state.value = ImportState.Idle
    }

    private fun mostRecentMonday(): LocalDate {
        var date = LocalDate.now()
        while (date.dayOfWeek.value != 1) date = date.minusDays(1)
        return date
    }
}
