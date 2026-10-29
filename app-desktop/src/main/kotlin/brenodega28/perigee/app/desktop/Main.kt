package brenodega28.perigee.app.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import brenodega28.perigee.ui.shared.PerigeePlaceholder

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Perigee") {
            PerigeePlaceholder()
        }
    }
