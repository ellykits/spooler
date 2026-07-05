package io.spooler.demo

import android.annotation.SuppressLint
import android.content.Context
import io.spooler.core.KmpPrintEngine

@SuppressLint("StaticFieldLeak") lateinit var demoAppContext: Context

actual fun createDemoEngine(): KmpPrintEngine = KmpPrintEngine(demoAppContext)
