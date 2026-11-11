package com.driveup.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.takahashirinta.kanesumi.anim.sokuou.SokuouTweens
import io.github.takahashirinta.kanesumi.core.insets.metroNavigationBarsPadding
import io.github.takahashirinta.kanesumi.core.insets.metroStatusBarsPadding
import io.github.takahashirinta.kanesumi.core.theme.LocalMetroColors
import kotlinx.coroutines.launch

/**
 * DriveUp 应用级 Metro 侧滑抽屉 —— Kanesumi 目前还没有抽屉组件,这是本 app 的
 * 私有形态(与 Ncrust 把 app 独有组件留在 app 内的做法一致)。
 *
 * 取代 M3 ModalNavigationDrawer + ModalDrawerSheet:
 * - 直角矩形面板,无 elevation,无默认 padding,内容侧按 Metro 贴边惯例排布;
 * - 单一 `progress: Animatable<Float>` (0=藏,1=显) 驱动 scrim alpha 与面板
 *   translationX,全部在 graphicsLayer 内读取 —— 严格零重组;
 * - 入场走 SokuouTweens.SheetAppear (300ms),收起走 SheetDismiss (260ms);
 * - 面板自身 pointerInput 消费 down,防止点空白区域穿透到 scrim 触发误关闭;
 *   行内 clickable 先于面板消费,交互不受影响。
 */
@Composable
fun MetroDrawer(
    drawerOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    drawerWidth: Dp = 280.dp,
    drawerColor: Color = LocalMetroColors.current.surface,
    scrimAlpha: Float = 0.4f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val progress = remember { Animatable(if (drawerOpen) 1f else 0f) }
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(drawerOpen) }

    LaunchedEffect(drawerOpen) {
        if (drawerOpen) {
            visible = true
            progress.animateTo(1f, SokuouTweens.SheetAppear)
        } else if (visible) {
            progress.animateTo(0f, SokuouTweens.SheetDismiss)
            visible = false
            onDismiss()
        }
    }

    if (!visible) return

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * scrimAlpha }
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    scope.launch {
                        progress.animateTo(0f, SokuouTweens.SheetDismiss)
                        visible = false
                        onDismiss()
                    }
                },
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(drawerWidth)
                .fillMaxHeight()
                // 面板挡住空白区域点击,避免穿透到下方 scrim 误关闭。用 no-op tap
                // 在 Main pass 消费 up —— scrim 的 clickable 只有看到未消费的 up 才会
                // 触发;行内 clickable 更早消费 up,no-op 不会干扰它们的点击。
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                }
                .graphicsLayer { translationX = -size.width + progress.value * size.width }
                .background(drawerColor),
        ) {
            // 内容可滚动,小屏不至于溢出;顶部/底部 inset 由抽屉自己消化。
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .metroStatusBarsPadding()
                    .metroNavigationBarsPadding(),
            ) {
                content()
            }
        }
    }
}
