package com.reasontouch.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalReasonTouchSkin =
    staticCompositionLocalOf<ReasonTouchSkin> {
        error("No ReasonTouchSkin provided")
    }

object ReasonTouchTheme {

    val skin: ReasonTouchSkin
        @Composable
        @ReadOnlyComposable
        get() = LocalReasonTouchSkin.current
}

@Composable
fun ReasonTouchTheme(
    skin: ReasonTouchSkin = NeoDarkSkin,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalReasonTouchSkin provides skin
    ) {
        content()
    }
}

