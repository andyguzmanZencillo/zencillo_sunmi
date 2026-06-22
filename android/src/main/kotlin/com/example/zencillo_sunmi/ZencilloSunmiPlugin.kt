package com.example.zencillo_sunmi

import android.content.Context
import android.os.RemoteException
import androidx.annotation.NonNull
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.sunmi.peripheral.printer.WoyouConsts
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** ZencilloSunmiPlugin */
class ZencilloSunmiPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    private var printerService: SunmiPrinterService? = null

    private val printerCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService?) {
            printerService = service
        }

        override fun onDisconnected() {
            printerService = null
        }
    }

    override fun onAttachedToEngine(
        @NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    ) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "zencillo_sunmi")
        channel.setMethodCallHandler(this)

        bindPrinterService()
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "bindPrinter" -> {
                val bindResult = bindPrinterService()
                result.success(bindResult)
            }

            "isConnected" -> {
                result.success(printerService != null)
            }

            "initPrinter" -> {
                initPrinter(result)
            }

            "printText" -> {
                val text = call.argument<String>("text") ?: ""
                val size = call.argument<Double>("size") ?: 24.0
                val align = call.argument<Int>("align") ?: 0
                val bold = call.argument<Boolean>("bold") ?: false

                printText(
                    text = text,
                    size = size.toFloat(),
                    align = align,
                    bold = bold,
                    result = result
                )
            }

            "printLine" -> {
                printLine(result)
            }

            "lineWrap" -> {
                val lines = call.argument<Int>("lines") ?: 3
                lineWrap(lines, result)
            }
            "feedPaper" -> {
                val lines = call.argument<Int>("lines") ?: 8
                feedPaper(lines, result)
            }

            "cutPaper" -> {
                cutPaper(result)
            }

            "printQr" -> {
                val data = call.argument<String>("data") ?: ""
                val size = call.argument<Int>("size") ?: 6
                printQr(data, size, result)
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        try {
            InnerPrinterManager.getInstance().unBindService(context, printerCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        channel.setMethodCallHandler(null)
    }

    private fun bindPrinterService(): Boolean {
        return try {
            InnerPrinterManager.getInstance().bindService(context, printerCallback)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun requirePrinter(result: Result): SunmiPrinterService? {
        val service = printerService

        if (service == null) {
            result.error(
                "PRINTER_NOT_CONNECTED",
                "La impresora Sunmi no está conectada.",
                null
            )
        }

        return service
    }

    private fun initPrinter(result: Result) {
        val service = requirePrinter(result) ?: return

        try {
            service.printerInit(null)
            setLineSpacingZero(service)
            result.success(true)
        } catch (e: RemoteException) {
            result.error("INIT_PRINTER_ERROR", e.message, null)
        }
    }

    private fun printText(
        text: String,
        size: Float,
        align: Int,
        bold: Boolean,
        result: Result
    ) {
        val service = requirePrinter(result) ?: return

        try {
            service.setAlignment(align, null)
            setLineSpacingZero(service)
            setBold(service, bold)

            val cleanText = text.trimEnd('\n', '\r')

            service.printTextWithFont(cleanText, null, size, null)
            service.lineWrap(1, null)

            if (bold) {
                setBold(service, false)
            }

            result.success(true)
        } catch (e: RemoteException) {
            result.error("PRINT_TEXT_ERROR", e.message, null)
        }
    }

    private fun printLine(result: Result) {
        val service = requirePrinter(result) ?: return

        try {
            setLineSpacingZero(service)
            service.printText("--------------------------------", null)
            service.lineWrap(1, null)
            result.success(true)
        } catch (e: RemoteException) {
            result.error("PRINT_LINE_ERROR", e.message, null)
        }
    }

    private fun lineWrap(lines: Int, result: Result) {
        val service = requirePrinter(result) ?: return

        try {
            service.lineWrap(lines, null)
            result.success(true)
        } catch (e: RemoteException) {
            result.error("LINE_WRAP_ERROR", e.message, null)
        }
    }

    private fun cutPaper(result: Result) {
        val service = requirePrinter(result) ?: return

        try {
            service.cutPaper(null)
            result.success(true)
        } catch (e: Exception) {
            result.error("CUT_PAPER_ERROR", e.message, null)
        }
    }
    
    private fun feedPaper(lines: Int, result: Result) {
        val service = requirePrinter(result) ?: return

        try {
            try {
                service.autoOutPaper(null)
            } catch (e: RemoteException) {
                service.lineWrap(lines, null)
            }

            result.success(true)
        } catch (e: Exception) {
            try {
                service.lineWrap(lines, null)
                result.success(true)
            } catch (ex: RemoteException) {
                result.error("FEED_PAPER_ERROR", ex.message, null)
            }
        }
    }

    private fun printQr(data: String, size: Int, result: Result) {
        val service = requirePrinter(result) ?: return

        try {
            service.setAlignment(1, null)
            setLineSpacingZero(service)
            service.printQRCode(data, size, 2, null)
            service.lineWrap(1, null)
            result.success(true)
        } catch (e: RemoteException) {
            result.error("PRINT_QR_ERROR", e.message, null)
        }
    }

    private fun setLineSpacingZero(service: SunmiPrinterService) {
        try {
            service.setPrinterStyle(WoyouConsts.SET_LINE_SPACING, 0)
        } catch (e: RemoteException) {
            service.sendRAWData(byteArrayOf(0x1B, 0x33, 0x00), null)
        }
    }

    private fun setBold(service: SunmiPrinterService, enabled: Boolean) {
        try {
            service.setPrinterStyle(
                WoyouConsts.ENABLE_BOLD,
                if (enabled) WoyouConsts.ENABLE else WoyouConsts.DISABLE
            )
        } catch (e: RemoteException) {
            if (enabled) {
                service.sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), null)
            } else {
                service.sendRAWData(byteArrayOf(0x1B, 0x45, 0x00), null)
            }
        }
    }
}