package com.driveup.ui.import_

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driveup.data.repository.TimetableRepository
import com.driveup.ui.theme.ErrorRed
import com.driveup.ui.theme.LocalAppColors
import io.github.takahashirinta.kanesumi.controls.MetroButton
import io.github.takahashirinta.kanesumi.controls.MetroProgressIndicator
import io.github.takahashirinta.kanesumi.core.insets.metroNavigationBarsPadding
import io.github.takahashirinta.kanesumi.core.insets.metroStatusBarsPadding
import io.github.takahashirinta.kanesumi.core.theme.LocalMetroTypography
import io.github.takahashirinta.kanesumi.core.theme.MetroIcon
import io.github.takahashirinta.kanesumi.core.theme.MetroText
import java.time.LocalDate

@Composable
fun ImportScreen(
    repository: TimetableRepository,
    onImportComplete: () -> Unit
) {
    val vm: ImportViewModel = viewModel(
        factory = remember(repository) {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ImportViewModel(repository) as T
                }
            }
        }
    )

    val state by vm.state.collectAsState()
    val startDate by vm.startDate.collectAsState()
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalMetroTypography.current

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedUri = it
            selectedFileName = it.lastPathSegment?.substringAfterLast('/') ?: "已选择文件"
        }
    }

    // Navigate away on success
    LaunchedEffect(state) {
        if (state is ImportState.Success) {
            onImportComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .metroStatusBarsPadding()
            .metroNavigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // App name
        MetroText(
            text = "Drive Up!",
            style = typography.pageHeading.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = colors.todayAccent,
        )
        MetroText(
            text = "导入课表开始使用",
            style = typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(48.dp))

        // File picker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .border(2.dp, if (selectedUri != null) colors.todayAccent else colors.gridLine)
                .background(colors.surface)
                .clickable { launcher.launch(arrayOf("application/vnd.ms-excel", "*/*")) },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MetroIcon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = if (selectedUri != null) colors.todayAccent else colors.textSecondary,
                    sizeDp = 36.dp,
                )
                Spacer(Modifier.height(8.dp))
                MetroText(
                    text = if (selectedUri != null) selectedFileName else "点击选择 .xls 文件",
                    style = typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    color = if (selectedUri != null) colors.textPrimary else colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Start date picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            vm.setStartDate(LocalDate.of(year, month + 1, day))
                        },
                        startDate.year,
                        startDate.monthValue - 1,
                        startDate.dayOfMonth
                    ).show()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetroIcon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = colors.todayAccent,
            )
            Column {
                MetroText(
                    text = "学期开始日期（第1周周一）",
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
                MetroText(
                    text = "${startDate.year}年${startDate.monthValue}月${startDate.dayOfMonth}日",
                    style = typography.titleMedium,
                    color = colors.textPrimary,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Import button — flat Metro tile
        if (state is ImportState.Loading) {
            // MetroButton 不支持 loading content,loading 态用同尺寸矩形 + 白 spinner 兜底。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(colors.todayAccent),
                contentAlignment = Alignment.Center,
            ) {
                MetroProgressIndicator(
                    color = Color.White,
                    sizeDp = 20.dp,
                    strokeDp = 2.dp,
                )
            }
        } else {
            MetroButton(
                text = "导入课表",
                onClick = {
                    selectedUri?.let { vm.importFile(context, it) }
                },
                enabled = selectedUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                containerColor = colors.todayAccent,
                textStyle = typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            )
        }

        // Error message
        if (state is ImportState.Error) {
            Spacer(Modifier.height(16.dp))
            MetroText(
                text = (state as ImportState.Error).message,
                style = typography.bodyMedium.copy(textAlign = TextAlign.Center),
                color = ErrorRed,
            )
        }
    }
}
