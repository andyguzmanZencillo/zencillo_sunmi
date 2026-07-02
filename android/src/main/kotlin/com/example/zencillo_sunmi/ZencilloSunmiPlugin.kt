package com.example.zencillo_sunmi

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.sunmi.peripheral.printer.WoyouConsts
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.atomic.AtomicBoolean

class ZencilloSunmiPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var channel: MethodChannel
    private var context: Context? = null
    private var service: SunmiPrinterService? = null
    private val binding = AtomicBoolean(false)

    private val callback = object : InnerPrinterCallback() {
        override fun onConnected(sunmiPrinterService: SunmiPrinterService) {
            service = sunmiPrinterService
            binding.set(false)
        }

        override fun onDisconnected() {
            service = null
            binding.set(false)
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL)
        channel.setMethodCallHandler(this)
        bindPrinterService()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        unbindPrinterService()
        context = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "init" -> {
                    bindPrinterService()
                    result.success(isConnected())
                }

                "isConnected" -> {
                    result.success(isConnected())
                }

                "sunmiPrint" -> {
                    val text = call.argument<String>("text") ?: ""
                    val size = call.argument<Int>("size") ?: 22

                    initPrintOriginal(text, size)

                    result.success(true)
                }

                "printText" -> {
                    val text = call.argument<String>("text") ?: ""
                    val size = (call.argument<Number>("size") ?: 22).toFloat()
                    val bold = call.argument<Boolean>("bold") ?: true
                    val underline = call.argument<Boolean>("underline") ?: false
                    val align = call.argument<Int>("align") ?: 1
                    val feedLines = call.argument<Int>("feedLines") ?: 6
                    val qrSize = call.argument<Int>("qrSize") ?: 5

                    printTextInternal(
                        input = text,
                        size = size,
                        bold = bold,
                        underline = underline,
                        align = align,
                        feedLines = feedLines,
                        qrSize = qrSize,
                    )

                    result.success(true)
                }

                "printLines" -> {
                    val lines = call.argument<List<String>>("lines") ?: emptyList()
                    val size = (call.argument<Number>("size") ?: 22).toFloat()
                    val bold = call.argument<Boolean>("bold") ?: true
                    val align = call.argument<Int>("align") ?: 1
                    val feedLines = call.argument<Int>("feedLines") ?: 6

                    printTextInternal(
                        input = lines.joinToString("\n") + "\n",
                        size = size,
                        bold = bold,
                        underline = false,
                        align = align,
                        feedLines = feedLines,
                        qrSize = 5,
                    )

                    result.success(true)
                }

                "printQr" -> {
                    val data = call.argument<String>("data") ?: ""
                    val size = call.argument<Int>("size") ?: 5
                    val errorLevel = call.argument<Int>("errorLevel") ?: 0

                    printQr(data, size, errorLevel)
                    result.success(true)
                }

                "feed" -> {
                    val lines = call.argument<Int>("lines") ?: 3
                    serviceOrThrow().lineWrap(lines, null)
                    result.success(true)
                }

                "cut" -> {
                    cutPaper()
                    result.success(true)
                }

                "openCashDrawer" -> {
                    serviceOrThrow().openDrawer(null)
                    result.success(true)
                }

                "getStatus" -> {
                    result.success(getStatus())
                }

                "getPrinterInfo" -> {
                    result.success(getPrinterInfo())
                }

                else -> result.notImplemented()
            }
        } catch (e: Exception) {
            result.error("SUNMI_ERROR", e.message ?: e.toString(), null)
        }
    }

    /**
     * Este metodo replica la logica original de PrintSunmy.java:
     *
     * - Crea un Thread.
     * - Intenta hasta 1000 veces alinear al centro.
     * - Si logra alinear, vuelve a setear alineacion varias veces.
     * - Extrae QR con !-QR-!codigo!-QR-!
     * - Imprime texto en bold.
     * - Imprime QR si existe.
     * - Imprime 6 saltos finales.
     */
    private fun initPrintOriginal(sContent: String, nSize: Int) {
        Thread {
            try {
                for (i in 0 until 1000) {
                    if (setAlign(1)) {
                        for (j in 1 until 10) {
                            setAlign(1)
                        }

                        val result = extractAndRemoveQrCodeUrl(sContent)

                        if (result.extractedCodeQR != null) {
                            printTextOriginal(
                                content = result.modifiedInput,
                                size = nSize.toFloat(),
                                isBold = true,
                                isUnderline = false,
                            )

                            printTextOriginal(
                                content = "  \n",
                                size = nSize.toFloat(),
                                isBold = false,
                                isUnderline = false,
                            )

                            printQr(
                                data = result.extractedCodeQR,
                                size = 5,
                                errorLevel = 0,
                            )
                        } else {
                            printTextOriginal(
                                content = sContent,
                                size = nSize.toFloat(),
                                isBold = true,
                                isUnderline = false,
                            )
                        }

                        for (x in 1..6) {
                            printTextOriginal(
                                content = "  \n",
                                size = nSize.toFloat(),
                                isBold = false,
                                isUnderline = false,
                            )
                        }

                        break
                    } else {
                        Thread.sleep(500)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun bindPrinterService() {
        val ctx = context ?: return
        if (service != null || binding.get()) return

        try {
            binding.set(true)
            val ok = InnerPrinterManager.getInstance().bindService(ctx, callback)
            if (!ok) binding.set(false)
        } catch (e: InnerPrinterException) {
            binding.set(false)
            Log.e(TAG, "No se pudo conectar al servicio Sunmi", e)
        }
    }

    private fun unbindPrinterService() {
        val ctx = context ?: return

        try {
            service?.let {
                InnerPrinterManager.getInstance().unBindService(ctx, callback)
            }
        } catch (e: InnerPrinterException) {
            Log.e(TAG, "No se pudo desconectar el servicio Sunmi", e)
        } finally {
            service = null
            binding.set(false)
        }
    }

    private fun isConnected(): Boolean {
        val s = service ?: return false

        return try {
            InnerPrinterManager.getInstance().hasPrinter(s)
        } catch (e: Exception) {
            false
        }
    }

    private fun serviceOrThrow(): SunmiPrinterService {
        val s = service

        if (s == null) {
            bindPrinterService()
            throw IllegalStateException("Impresora Sunmi no conectada. Llama init() y vuelve a intentar.")
        }

        return s
    }

    private fun setAlign(align: Int): Boolean {
        return try {
            val s = service ?: run {
                bindPrinterService()
                return false
            }

            s.setAlignment(align, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun printTextOriginal(
        content: String,
        size: Float,
        isBold: Boolean,
        isUnderline: Boolean,
    ) {
        val s = serviceOrThrow()

        setLineSpacingZero(s)
        setBold(s, isBold)
        setUnderline(s, isUnderline)

        s.printTextWithFont(content, null, size, null)
    }

    private fun printTextInternal(
        input: String,
        size: Float,
        bold: Boolean,
        underline: Boolean,
        align: Int,
        feedLines: Int,
        qrSize: Int,
    ) {
        val s = serviceOrThrow()
        val qrResult = extractAndRemoveQr(input)

        s.printerInit(null)
        s.setAlignment(align, null)
        setLineSpacingZero(s)
        setBold(s, bold)
        setUnderline(s, underline)

        s.printTextWithFont(qrResult.text, null, size, null)

        if (qrResult.qr != null) {
            s.printTextWithFont("  \n", null, size, null)
            s.setAlignment(1, null)
            s.printQRCode(qrResult.qr, qrSize, 0, null)
        }

        if (feedLines > 0) {
            s.lineWrap(feedLines, null)
        }
    }

    private fun printQr(data: String, size: Int, errorLevel: Int) {
        val s = serviceOrThrow()

        s.setAlignment(1, null)
        s.printQRCode(data, size, errorLevel, null)
    }

    private fun cutPaper() {
        try {
            serviceOrThrow().cutPaper(null)
        } catch (e: RemoteException) {
            serviceOrThrow().lineWrap(3, null)
        }
    }

    private fun setLineSpacingZero(s: SunmiPrinterService) {
        try {
            s.setPrinterStyle(WoyouConsts.SET_LINE_SPACING, 0)
        } catch (e: Exception) {
            s.sendRAWData(byteArrayOf(0x1B, 0x33, 0x00), null)
        }
    }

    private fun setBold(s: SunmiPrinterService, enabled: Boolean) {
        try {
            s.setPrinterStyle(
                WoyouConsts.ENABLE_BOLD,
                if (enabled) WoyouConsts.ENABLE else WoyouConsts.DISABLE,
            )
        } catch (e: Exception) {
            s.sendRAWData(if (enabled) ESCUtil.boldOn() else ESCUtil.boldOff(), null)
        }
    }

    private fun setUnderline(s: SunmiPrinterService, enabled: Boolean) {
        try {
            s.setPrinterStyle(
                WoyouConsts.ENABLE_UNDERLINE,
                if (enabled) WoyouConsts.ENABLE else WoyouConsts.DISABLE,
            )
        } catch (e: Exception) {
            s.sendRAWData(if (enabled) ESCUtil.underlineOn() else ESCUtil.underlineOff(), null)
        }
    }

    private fun getStatus(): Map<String, Any?> {
        val s = service ?: return mapOf(
            "code" to 505,
            "message" to "printer does not exist or service is not connected",
            "connected" to false,
        )

        val code = try {
            s.updatePrinterState()
        } catch (e: Exception) {
            -1
        }

        return mapOf(
            "code" to code,
            "message" to printerStatusMessage(code),
            "connected" to isConnected(),
        )
    }

    private fun getPrinterInfo(): Map<String, Any?> {
        val s = service ?: return mapOf("connected" to false)

        return mapOf(
            "connected" to isConnected(),
            "serialNo" to safeString { s.printerSerialNo },
            "model" to safeString { s.printerModal },
            "version" to safeString { s.printerVersion },
            "paper" to safeString { if (s.printerPaper == 1) "58mm" else "80mm" },
        )
    }

    private fun safeString(block: () -> String?): String {
        return try {
            block().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun printerStatusMessage(code: Int): String {
        return when (code) {
            1 -> "printer is running"
            2 -> "printer found but still initializing"
            3 -> "printer hardware interface is abnormal and needs to be reprinted"
            4 -> "printer is out of paper"
            5 -> "printer is overheating"
            6 -> "printer cover is not closed"
            7 -> "printer cutter is abnormal"
            8 -> "printer cutter is normal"
            9 -> "not found black mark paper"
            505 -> "printer does not exist"
            -1 -> "could not read printer status"
            else -> "unknown status"
        }
    }

    private fun extractAndRemoveQr(input: String): QrResult {
        val regex = Regex("!-QR-!(.*?)!-QR-!", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(input)
        val qr = match?.groups?.get(1)?.value
        val text = if (match != null) input.replaceRange(match.range, "") else input

        return QrResult(text = text, qr = qr)
    }

    private fun extractAndRemoveQrCodeUrl(input: String): ResultPrint {
        val regex = Regex("!-QR-!(.*?)!-QR-!", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(input)

        if (match != null) {
            val extractedCodeQR = match.groups[1]?.value
            val modifiedInput = input.replaceRange(match.range, "")

            return ResultPrint(
                modifiedInput = modifiedInput,
                extractedCodeQR = extractedCodeQR,
            )
        }

        return ResultPrint(
            modifiedInput = input,
            extractedCodeQR = null,
        )
    }

    private data class QrResult(
        val text: String,
        val qr: String?,
    )

    private data class ResultPrint(
        val modifiedInput: String,
        val extractedCodeQR: String?,
    )

    private object ESCUtil {
        private const val ESC: Byte = 0x1B

        fun boldOn(): ByteArray = byteArrayOf(ESC, 69, 1)
        fun boldOff(): ByteArray = byteArrayOf(ESC, 69, 0)
        fun underlineOn(): ByteArray = byteArrayOf(ESC, 45, 1)
        fun underlineOff(): ByteArray = byteArrayOf(ESC, 45, 0)
    }

    companion object {
        private const val CHANNEL = "zencillo_sunmi"
        private const val TAG = "ZencilloSunmiPlugin"
    }
}