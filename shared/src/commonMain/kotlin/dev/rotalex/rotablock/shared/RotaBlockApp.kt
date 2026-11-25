package dev.rotalex.rotablock.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.rotalex.rotablock.ui.block.BlockCanvas
import dev.rotalex.rotablock.ui.block.BlockOutline
import dev.rotalex.rotablock.ui.block.outlineRows
import androidx.compose.ui.tooling.preview.Preview

/**
 * The embeddable RotaBlock application surface: the document's outline beside the blocks it projects.
 *
 * A host renders this composable inside its own window or activity; everything the framework needs is
 * composed behind it. It owns no document state of its own and never parses source text.
 */
@Composable
fun RotaBlockApp() {
    MaterialTheme {
        RotaBlockWorkspace(document = SAMPLE_DOCUMENT)
    }
}

@Composable
private fun RotaBlockWorkspace(document: String) {
    val tree = remember(document) { JsonWorkspace.project(document) }
    val rows = remember(tree) { outlineRows(tree.root) }
    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = OUTLINE_BACKGROUND,
            modifier = Modifier.fillMaxHeight().weight(0.45f),
        ) {
            BlockOutline(
                rows = rows,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        BlockCanvas(tree = tree, modifier = Modifier.fillMaxHeight().weight(0.55f))
    }
}

private val OUTLINE_BACKGROUND = Color(0xFFFAF7EF)

private val SAMPLE_DOCUMENT = """
    {
      "path": "/greeting",
      "method": "GET",
      "handler": {
        "name": "greeting",
        "status": 200,
        "body": {
          "message": "Hello",
          "items": [1, 2, 3]
        }
      }
    }
""".trimIndent()

@Preview(widthDp = 1100, heightDp = 700)
@Composable
private fun RotaBlockAppPreview() {
    RotaBlockApp()
}
