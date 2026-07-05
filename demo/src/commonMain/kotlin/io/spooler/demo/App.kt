package io.spooler.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.spooler.core.DocumentType
import io.spooler.core.EscPosDriver
import io.spooler.core.PrintEngine
import io.spooler.core.PrintTarget
import io.spooler.core.isSuccess
import io.spooler.core.print
import kotlinx.coroutines.launch

@Composable
fun App(engine: PrintEngine) {
  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      val scope = rememberCoroutineScope()
      var status by remember { mutableStateOf("Ready") }
      Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("spooler demo", style = MaterialTheme.typography.headlineSmall)
        for (sample in allSamples()) {
          Button(
            onClick = {
              scope.launch {
                val target =
                  if (sample.document.type.isContinuous) {
                    val narrow = sample.document.type == DocumentType.RECEIPT_58MM
                    PrintTarget.SendToPrinter(
                      EscPosDriver(
                        paperWidthMm = if (narrow) 58 else 80,
                        charactersPerLine = if (narrow) 32 else 48,
                      )
                    )
                  } else {
                    PrintTarget.SaveToFile("${sample.label.replace(" ", "-")}.pdf")
                  }
                val result = engine.print(sample.document, target)
                status =
                  if (result.isSuccess) {
                    "${sample.label}: ✓ $result"
                  } else {
                    "${sample.label}: ✗ $result"
                  }
              }
            }
          ) {
            Text(sample.label)
          }
        }
        Text(status, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}
