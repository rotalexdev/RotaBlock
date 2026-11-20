package dev.rotalex.rotablock.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The embeddable RotaBlock application surface.
 *
 * A host renders this composable inside its own window or activity; everything the framework needs is
 * composed behind it. It owns no document state of its own and never parses source text.
 */
@Composable
fun RotaBlockApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("RotaBlock")
            }
        }
    }
}
