package com.github.cdahmad.cop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
@Preview
fun TextCompose() {
    Text(text = "Hello World")
}


val LocalColor = compositionLocalOf { Color.Gray }

@Composable
@Preview
fun AnimCompose(default: Boolean = true) {
    val big = remember {
        mutableStateOf(default)
    }
    val size by animateDpAsState(targetValue = if (big.value) 90.dp else 50.dp)
    Text(
        text = "Hello World", modifier = Modifier
            .background(color = LocalColor.current)
            .padding(10.dp)
            .size(size)
            .clickable {
                big.value = !big.value
            }, textAlign = TextAlign.Center
    )
}


@Composable
@Preview
fun AnimatableCompose(default: Boolean = true) {
    var big = remember { mutableStateOf(default) }
    val anim = remember { Animatable(48.dp, Dp.VectorConverter) }
    val scope = rememberCoroutineScope()
    Text(
        text = "Hello World", modifier = Modifier
            .padding(10.dp)
            .background(color = LocalColor.current)
            .padding(10.dp)
            .size(anim.value)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(color = Color.Blue, bounded = false)) {
                scope.launch {
                    // 1. 先瞬间变大 (或者也可以 animateTo 变大)
                    anim.snapTo(200.dp)

                    // 2. 再根据新的 big 状态动画回去
                    val targetSize = if (!big.value) 90.dp else 50.dp // 注意这里取反，因为 big 还没变
                    anim.animateTo(
                        targetValue = targetSize,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )

                    // 3. 最后更新状态 (确保 UI 其他部分同步)
                    big.value = !big.value
                }
            }, textAlign = TextAlign.Center
    )
}

