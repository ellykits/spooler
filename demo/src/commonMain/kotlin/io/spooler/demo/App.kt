/*
* Copyright 2026 Spooler Contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.spooler.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.spooler.core.DocumentType
import io.spooler.core.EscPosDriver
import io.spooler.core.PrintEngine
import io.spooler.core.PrintResult
import io.spooler.core.PrintTarget
import io.spooler.core.print
import kotlinx.coroutines.launch

private val InkTeal = Color(0xFF0F766E)
private val KilnAmber = Color(0xFFB45309)

private data class ActionStatus(val text: String, val isError: Boolean)

@Composable
fun App(engine: PrintEngine) {
  DemoTheme {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      val scope = rememberCoroutineScope()
      val samples = remember { allSamples() }
      var status by remember { mutableStateOf<ActionStatus?>(null) }

      Column(
        modifier =
          Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Header()
        Spacer(Modifier.height(28.dp))
        Column(
          modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          for (sample in samples) {
            DocumentCard(
              sample = sample,
              onPreview = { openPreview(sample.document.buildHtml()) },
              onAction = { scope.launch { status = printSample(engine, sample) } },
            )
          }
        }
        status?.let {
          Spacer(Modifier.height(20.dp))
          StatusChip(it)
        }
      }
    }
  }
}

@Composable
private fun Header() {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      "spooler",
      style = MaterialTheme.typography.headlineMedium,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp,
      color = InkTeal,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      "Print receipts and documents from one Kotlin Multiplatform API",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun DocumentCard(sample: Sample, onPreview: () -> Unit, onAction: () -> Unit) {
  val type = sample.document.type
  val accent = if (type.isContinuous) KilnAmber else InkTeal
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
      Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
      Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          PaperGlyph(accent, type.isContinuous)
          Spacer(Modifier.width(14.dp))
          Column {
            Text(
              sample.label,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
              formatTag(type),
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.8.sp,
              color = accent,
            )
          }
        }
        Spacer(Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
          OutlinedButton(onClick = onPreview) { Text("Preview") }
          Button(onClick = onAction) { Text(if (type.isContinuous) "Print" else "Save PDF") }
        }
      }
    }
  }
}

/** A miniature of the document's real paper: a narrow strip for receipts, a sheet for A4. */
@Composable
private fun PaperGlyph(accent: Color, continuous: Boolean) {
  Canvas(Modifier.size(32.dp, 40.dp)) {
    val paperWidth = size.width * if (continuous) 0.46f else 0.78f
    val left = (size.width - paperWidth) / 2f
    val top = size.height * 0.05f
    val paperHeight = size.height * 0.9f
    drawRoundRect(
      color = accent,
      topLeft = Offset(left, top),
      size = Size(paperWidth, paperHeight),
      cornerRadius = CornerRadius(2.dp.toPx()),
      style = Stroke(width = 1.5.dp.toPx()),
    )
    val inset = paperWidth * 0.2f
    val line = accent.copy(alpha = 0.45f)
    for (i in 0..2) {
      val y = top + paperHeight * (0.3f + i * 0.2f)
      drawLine(line, Offset(left + inset, y), Offset(left + paperWidth - inset, y), 1.2.dp.toPx())
    }
  }
}

@Composable
private fun StatusChip(status: ActionStatus) {
  val container =
    if (status.isError) {
      MaterialTheme.colorScheme.errorContainer
    } else {
      MaterialTheme.colorScheme.primaryContainer
    }
  val content =
    if (status.isError) {
      MaterialTheme.colorScheme.onErrorContainer
    } else {
      MaterialTheme.colorScheme.onPrimaryContainer
    }
  Surface(shape = RoundedCornerShape(10.dp), color = container) {
    Text(
      status.text,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
      style = MaterialTheme.typography.bodyMedium,
      color = content,
    )
  }
}

private fun formatTag(type: DocumentType): String =
  when (type) {
    DocumentType.A4_DOCUMENT -> "A4 · PDF"
    DocumentType.RECEIPT_80MM -> "80MM · THERMAL"
    DocumentType.RECEIPT_58MM -> "58MM · THERMAL"
  }

private suspend fun printSample(engine: PrintEngine, sample: Sample): ActionStatus {
  val type = sample.document.type
  val target =
    if (type.isContinuous) {
      val narrow = type == DocumentType.RECEIPT_58MM
      PrintTarget.SendToPrinter(
        EscPosDriver(
          paperWidthMm = if (narrow) 58 else 80,
          charactersPerLine = if (narrow) 32 else 48,
        )
      )
    } else {
      PrintTarget.SaveToFile("${sample.label.replace(" ", "-")}.pdf")
    }
  return when (val result = engine.print(sample.document, target)) {
    is PrintResult.Saved -> ActionStatus("Saved ${result.path}", isError = false)
    PrintResult.Success -> ActionStatus("Sent to printer", isError = false)
    is PrintResult.Failure -> ActionStatus(result.message, isError = true)
  }
}

@Composable
private fun DemoTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme =
      lightColorScheme(
        primary = InkTeal,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCDE9E4),
        onPrimaryContainer = Color(0xFF00201C),
        background = Color(0xFFF7F8F7),
        surface = Color.White,
        surfaceVariant = Color(0xFFEFF1F0),
        onSurfaceVariant = Color(0xFF5A6260),
        outlineVariant = Color(0xFFDADEDD),
      ),
    content = content,
  )
}
