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
package io.spooler.core

import java.net.ServerSocket
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class NetworkPrinterTest {
  @Test
  fun sendsEscposBytesToAListeningSocket() = runTest {
    val server = ServerSocket(0)
    val received = async(Dispatchers.IO) { server.accept().use { it.getInputStream().readBytes() } }

    val result =
      withContext(Dispatchers.IO) {
        sendToNetworkPrinter("HI".encodeToByteArray(), "127.0.0.1", server.localPort)
      }

    assertTrue(result is PrintResult.Success)
    assertEquals("HI", received.await().decodeToString())
    server.close()
  }

  @Test
  fun reportsFailureWhenNothingIsListening() = runTest {
    // Port 1 is privileged; nothing can bind it without root, so the connection
    // is refused deterministically instead of racing a recycled ephemeral port.
    val result =
      withContext(Dispatchers.IO) { sendToNetworkPrinter("HI".encodeToByteArray(), "127.0.0.1", 1) }

    assertTrue(result is PrintResult.Failure)
  }

  @Test
  fun reportsFailureWhenThePrinterDropsTheConnection() = runTest {
    val server = ServerSocket(0)
    // SO_LINGER 0 makes close send an RST, so the payload cannot drain.
    val dropped = async(Dispatchers.IO) { server.accept().use { it.setSoLinger(true, 0) } }

    val result =
      withContext(Dispatchers.IO) {
        sendToNetworkPrinter(ByteArray(16 * 1024 * 1024), "127.0.0.1", server.localPort)
      }

    assertTrue(result is PrintResult.Failure)
    dropped.await()
    server.close()
  }

  @Test
  fun letsACancelledCallerCancelInsteadOfReportingFailure() = runTest {
    val server = ServerSocket(0)
    val accepted = CompletableDeferred<Socket>()
    val acceptor = async(Dispatchers.IO) { accepted.complete(server.accept()) }
    val result = CompletableDeferred<PrintResult>()
    val send =
      launch(Dispatchers.IO) {
        result.complete(
          sendToNetworkPrinter(ByteArray(16 * 1024 * 1024), "127.0.0.1", server.localPort)
        )
      }

    val socket = accepted.await()
    send.cancelAndJoin()

    assertFalse(result.isCompleted)
    socket.close()
    acceptor.await()
    server.close()
  }

  @Test
  fun mapsDriverFieldsThroughTheSharedEscposBuilder() {
    val bytes =
      NetworkEscPosDriver(host = "h", charactersPerLine = 32, cut = false, openDrawer = true)
        .toEscPosBytes("<p>hi</p>")

    assertContentEquals(
      buildEscPos(
        htmlToText("<p>hi</p>"),
        EscPosDriver(charactersPerLine = 32, cut = false, openDrawer = true),
      ),
      bytes,
    )
  }
}
