package io.spooler.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import io.spooler.core.KmpPrintEngine
import io.spooler.core.PrintTarget
import io.spooler.core.StandardSystemDriver
import kotlinx.coroutines.launch

@Composable
fun App(engine: KmpPrintEngine) {
  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      val scope = rememberCoroutineScope()
      var status by remember { mutableStateOf("Ready") }
      Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("spooler demo", style = MaterialTheme.typography.headlineSmall)
        Button(
          onClick = {
            scope.launch {
              val result =
                engine.execute(
                  receiptHtml(),
                  PrintTarget.SendToPrinter(EscPosDriver(paperWidthMm = 80)),
                  DocumentType.RECEIPT_80MM,
                )
              status = "Receipt: $result"
            }
          }
        ) {
          Text("Print 80mm Receipt")
        }
        Button(
          onClick = {
            scope.launch {
              val result =
                engine.execute(
                  invoiceHtml(),
                  PrintTarget.SaveToFile("invoice-INV-2026-0042.pdf"),
                  DocumentType.A4_DOCUMENT,
                )
              status = "Invoice: $result"
            }
          }
        ) {
          Text("Export A4 Invoice")
        }
        Button(
          onClick = {
            scope.launch {
              val result =
                engine.execute(
                  invoiceHtml(),
                  PrintTarget.SendToPrinter(StandardSystemDriver()),
                  DocumentType.A4_DOCUMENT,
                )
              status = "Invoice print: $result"
            }
          }
        ) {
          Text("Print A4 Invoice")
        }
        Text(status, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}
