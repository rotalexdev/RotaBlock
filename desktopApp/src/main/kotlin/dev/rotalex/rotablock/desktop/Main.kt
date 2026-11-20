package dev.rotalex.rotablock.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.rotalex.rotablock.shared.RotaBlockApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "RotaBlock",
        state = rememberWindowState(size = DpSize(1280.dp, 800.dp)),
    ) {
        RotaBlockApp()
    }
}
