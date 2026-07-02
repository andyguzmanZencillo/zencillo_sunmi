package com.example.zencillo_sunmi

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import androidx.annotation.NonNull
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.sunmi.peripheral.printer.WoyouConsts
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.regex.Pattern

class ZencilloSunmiPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    companion object {
        private const val TAG = "ZencilloSunmiPlugin"
        private const val CHANNEL_NAME = "zencillo_sunmi"

        private const val NO_SUNMI_PRINTER = 0x00000000
        private const val CHECK_SUNMI_PRINTER = 0x00000001
        private const val FOUND_SUNMI_PRINTER = 0x00000002
        private const val LOST_SUNMI_PRINTER = 0x00000003
    }

    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context

    private var activityContext: Context? = null
    private var sunmiPrinterService: SunmiPrinterService? = null
    private var sunmiPrinterStatus: Int = CHECK_SUNMI_PRINTER

    private val mainHandler = Handler(Looper.getMainLooper())

    private val printerCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService?) {
            Log.d(TAG, "SUNMI onConnected")

            sunmiPrinterService = service

            if (service != null) {
                checkSunmiPrinterService(service)
            } else {
                sunmiPrinterStatus = NO_SUNMI_PRINTER
            }
        }

        override fun onDisconnected() {
            Log.d(TAG, "SUNMI onDisconnected")

            sunmiPrinterService = null
            sunmiPrinterStatus = LOST_SUNMI_PRINTER
        }
    }

    override fun onAttachedToEngine(
        @NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    ) {
        applicationContext = flutterPluginBinding.applicationContext

        channel = MethodChannel(
            flutterPluginBinding.binaryMessenger,
            CHANNEL_NAME
        )

        channel.setMethodCallHandler(this)

        initSunmiPrinterService()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        deInitSunmiPrinterService()
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityContext = binding.activity
        initSunmiPrinterService()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activityContext = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activityContext = binding.activity
        initSunmiPrinterService()
    }

    override fun onDetachedFromActivity() {
        activityContext = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "bindPrinter" -> {
                bindPrinter(result)
            }

            "isConnected" -> {
                result.success(isPrinterReady())
            }

            "initPrinter" -> {
                withReadyPrinter(result) { service ->
                    service.printerInit(null)
                    result.success(true)
                }
            }

            "printText" -> {
                val text = call.argument<String>("text") ?: ""
                val size = (call.argument<Number>("size") ?: 24).toFloat()
                val align = call.argument<Int>("align") ?: 0
                val bold = call.argument<Boolean>("bold") ?: false

                withReadyPrinter(result) { service ->
                    setAlign(service, align)

                    // Parecido al original:
                    // SunmiPrintHelper.printText(content, size, isBold, isUnderLine)
                    printTextOriginalStyle(
                        service = service,
                        content = text,
                        size = size,
                        isBold = bold,
                        isUnderline = false
                    )

                    // Compatibilidad con tu Flutter actual:
                    // tu app manda una línea por llamada a printText().
                    service.lineWrap(1, null)

                    result.success(true)
                }
            }

            "printLine" -> {
                withReadyPrinter(result) { service ->
                    setAlign(service, 0)
                    printTextOriginalStyle(
                        service = service,
                        content = "--------------------------------",
                        size = 24f,
                        isBold = false,
                        isUnderline = false
                    )
                    service.lineWrap(1, null)
                    result.success(true)
                }
            }

            "lineWrap" -> {
                val lines = call.argument<Int>("lines") ?: 3

                withReadyPrinter(result) { service ->
                    service.lineWrap(lines, null)
                    result.success(true)
                }
            }

            "feedPaper" -> {
                val lines = call.argument<Int>("lines") ?: 8

                withReadyPrinter(result) { service ->
                    try {
                        service.autoOutPaper(null)
                    } catch (e: RemoteException) {
                        service.lineWrap(lines, null)
                    }
                    result.success(true)
                }
            }

            "cutPaper" -> {
                withReadyPrinter(result) { service ->
                    try {
                        service.cutPaper(null)
                    } catch (e: Exception) {
                        Log.e(TAG, "cutPaper failed", e)
                    }
                    result.success(true)
                }
            }

            "printQr" -> {
                val data = call.argument<String>("data") ?: ""
                val size = call.argument<Int>("size") ?: 5

                if (data.isBlank()) {
                    result.error(
                        "INVALID_QR",
                        "El contenido del QR está vacío.",
                        null
                    )
                    return
                }

                withReadyPrinter(result) { service ->
                    setAlign(service, 1)

                    // Igual que la librería original:
                    // printQr(data, 5, 0)
                    service.printQRCode(data, size, 0, null)
                    service.lineWrap(1, null)

                    result.success(true)
                }
            }

            "printImageBytes" -> {
                val bytes = call.argument<ByteArray>("bytes")

                if (bytes == null || bytes.isEmpty()) {
                    result.error(
                        "INVALID_IMAGE",
                        "Los bytes de la imagen están vacíos.",
                        null
                    )
                    return
                }

                withReadyPrinter(result) { service ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    if (bitmap == null) {
                        result.error(
                            "INVALID_IMAGE",
                            "No se pudo decodificar la imagen.",
                            null
                        )
                        return@withReadyPrinter
                    }

                    setAlign(service, 1)
                    service.printBitmap(bitmap, null)
                    service.lineWrap(1, null)

                    result.success(true)
                }
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    private fun getSafeContext(): Context {
        return activityContext ?: applicationContext
    }

    /**
     * Parecido a:
     * SunmiPrintHelper.getInstance().initSunmiPrinterService(context)
     */
    private fun initSunmiPrinterService(): Boolean {
        return try {
            var ret = false

            for (attempt in 0..50) {
                ret = InnerPrinterManager.getInstance()
                    .bindService(getSafeContext(), printerCallback)

                if (ret) {
                    Log.d(TAG, "SUNMI bindService OK attempt=$attempt")
                    break
                }
            }

            if (!ret) {
                sunmiPrinterStatus = NO_SUNMI_PRINTER
            }

            ret
        } catch (e: InnerPrinterException) {
            Log.e(TAG, "SUNMI bindService error", e)
            sunmiPrinterStatus = NO_SUNMI_PRINTER
            false
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI bindService unexpected error", e)
            sunmiPrinterStatus = NO_SUNMI_PRINTER
            false
        }
    }

    private fun deInitSunmiPrinterService() {
        try {
            if (sunmiPrinterService != null) {
                InnerPrinterManager.getInstance()
                    .unBindService(getSafeContext(), printerCallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI unBindService error", e)
        } finally {
            sunmiPrinterService = null
            sunmiPrinterStatus = LOST_SUNMI_PRINTER
        }
    }

    /**
     * Parecido a:
     * checkSunmiPrinterService(service)
     */
    private fun checkSunmiPrinterService(service: SunmiPrinterService) {
        val hasPrinter = try {
            InnerPrinterManager.getInstance().hasPrinter(service)
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI hasPrinter error", e)
            false
        }

        sunmiPrinterStatus = if (hasPrinter) {
            FOUND_SUNMI_PRINTER
        } else {
            NO_SUNMI_PRINTER
        }

        Log.d(TAG, "SUNMI printer status=$sunmiPrinterStatus")
    }

    /**
     * Antes tu bindPrinter devolvía true inmediatamente.
     * Ahora espera hasta que setAlign(1) funcione, igual que el Java original.
     */
    private fun bindPrinter(result: MethodChannel.Result) {
        if (isPrinterReady()) {
            result.success(true)
            return
        }

        val started = initSunmiPrinterService()

        if (!started && sunmiPrinterService == null) {
            result.error(
                "PRINTER_BIND_FAILED",
                "No se pudo enlazar con el servicio de impresora Sunmi.",
                null
            )
            return
        }

        waitUntilPrinterReady(
            maxAttempts = 50,
            delayMs = 200L,
            onReady = {
                result.success(true)
            },
            onTimeout = {
                result.error(
                    "PRINTER_CONNECTION_TIMEOUT",
                    "La impresora Sunmi no se conectó dentro del tiempo esperado.",
                    null
                )
            }
        )
    }

    private fun isPrinterReady(): Boolean {
        val service = sunmiPrinterService ?: return false

        return try {
            val hasPrinter = InnerPrinterManager.getInstance().hasPrinter(service)
            hasPrinter && setAlign(service, 1)
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI isPrinterReady error", e)
            false
        }
    }

    /**
     * Este método hace que tus métodos actuales de Flutter sean seguros.
     * Si Flutter llama printText() muy rápido, Kotlin espera la conexión.
     */
    private fun withReadyPrinter(
        result: MethodChannel.Result,
        action: (SunmiPrinterService) -> Unit
    ) {
        val service = sunmiPrinterService

        if (service != null && setAlign(service, 1)) {
            try {
                action(service)
            } catch (e: RemoteException) {
                result.error(
                    "SUNMI_REMOTE_ERROR",
                    e.message ?: "Error remoto de impresora Sunmi.",
                    null
                )
            } catch (e: Exception) {
                result.error(
                    "SUNMI_PRINT_ERROR",
                    e.message ?: "Error imprimiendo en Sunmi.",
                    null
                )
            }
            return
        }

        val started = initSunmiPrinterService()

        if (!started && sunmiPrinterService == null) {
            result.error(
                "PRINTER_NOT_CONNECTED",
                "La impresora Sunmi no está conectada.",
                null
            )
            return
        }

        waitUntilPrinterReady(
            maxAttempts = 50,
            delayMs = 200L,
            onReady = {
                val readyService = sunmiPrinterService

                if (readyService == null) {
                    result.error(
                        "PRINTER_NOT_CONNECTED",
                        "La impresora Sunmi no está conectada.",
                        null
                    )
                    return@waitUntilPrinterReady
                }

                try {
                    action(readyService)
                } catch (e: RemoteException) {
                    result.error(
                        "SUNMI_REMOTE_ERROR",
                        e.message ?: "Error remoto de impresora Sunmi.",
                        null
                    )
                } catch (e: Exception) {
                    result.error(
                        "SUNMI_PRINT_ERROR",
                        e.message ?: "Error imprimiendo en Sunmi.",
                        null
                    )
                }
            },
            onTimeout = {
                result.error(
                    "PRINTER_CONNECTION_TIMEOUT",
                    "La impresora Sunmi no respondió a tiempo.",
                    null
                )
            }
        )
    }

    /**
     * Igual que el Java original:
     * antes de imprimir prueba setAlign(1).
     */
    private fun waitUntilPrinterReady(
        maxAttempts: Int,
        delayMs: Long,
        attempt: Int = 0,
        onReady: () -> Unit,
        onTimeout: () -> Unit
    ) {
        val service = sunmiPrinterService

        if (service != null && setAlign(service, 1)) {
            onReady()
            return
        }

        if (attempt >= maxAttempts) {
            onTimeout()
            return
        }

        mainHandler.postDelayed({
            waitUntilPrinterReady(
                maxAttempts = maxAttempts,
                delayMs = delayMs,
                attempt = attempt + 1,
                onReady = onReady,
                onTimeout = onTimeout
            )
        }, delayMs)
    }

    /**
     * Parecido a:
     * SunmiPrintHelper.setAlign(int align)
     */
    private fun setAlign(service: SunmiPrinterService, align: Int): Boolean {
        return try {
            service.setAlignment(align, null)
            Log.d(TAG, "SUNMI setAlign OK align=$align")
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "SUNMI setAlign RemoteException", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI setAlign Exception", e)
            false
        }
    }

    /**
     * Parecido a:
     * SunmiPrintHelper.printText(content, size, isBold, isUnderLine)
     */
    private fun printTextOriginalStyle(
        service: SunmiPrinterService,
        content: String,
        size: Float,
        isBold: Boolean,
        isUnderline: Boolean
    ) {
        setLineSpacingZero(service)
        setBold(service, isBold)
        setUnderline(service, isUnderline)

        // Importante:
        // No hacemos trimEnd aquí.
        // La librería original imprimía el content tal cual llegaba.
        service.printTextWithFont(content, null, size, null)
    }

    private fun setLineSpacingZero(service: SunmiPrinterService) {
        try {
            service.setPrinterStyle(WoyouConsts.SET_LINE_SPACING, 0)
        } catch (e: RemoteException) {
            service.sendRAWData(byteArrayOf(0x1B, 0x33, 0x00), null)
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI setLineSpacingZero error", e)
        }
    }

    private fun setBold(service: SunmiPrinterService, enabled: Boolean) {
        try {
            service.setPrinterStyle(
                WoyouConsts.ENABLE_BOLD,
                if (enabled) WoyouConsts.ENABLE else WoyouConsts.DISABLE
            )
        } catch (e: RemoteException) {
            val command = if (enabled) {
                byteArrayOf(0x1B, 0x45, 0x01)
            } else {
                byteArrayOf(0x1B, 0x45, 0x00)
            }

            service.sendRAWData(command, null)
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI setBold error", e)
        }
    }

    private fun setUnderline(service: SunmiPrinterService, enabled: Boolean) {
        try {
            service.setPrinterStyle(
                WoyouConsts.ENABLE_UNDERLINE,
                if (enabled) WoyouConsts.ENABLE else WoyouConsts.DISABLE
            )
        } catch (e: RemoteException) {
            val command = if (enabled) {
                byteArrayOf(0x1B, 0x2D, 0x01)
            } else {
                byteArrayOf(0x1B, 0x2D, 0x00)
            }

            service.sendRAWData(command, null)
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI setUnderline error", e)
        }
    }

    /**
     * Lo dejo preparado por si luego quieres usar el formato original:
     * !-QR-!contenido_qr!-QR-!
     *
     * No rompe tu Flutter actual.
     */
    private fun extractAndRemoveQrCodeUrl(input: String): QrExtractionResult {
        val pattern = Pattern.compile("!-QR-!(.*?)!-QR-!")
        val matcher = pattern.matcher(input)

        var extractedQr: String? = null
        var modifiedInput = input

        if (matcher.find()) {
            extractedQr = matcher.group(1)
            modifiedInput = matcher.replaceFirst("")
        }

        return QrExtractionResult(
            extractedCodeQR = extractedQr,
            modifiedInput = modifiedInput
        )
    }

    private data class QrExtractionResult(
        val extractedCodeQR: String?,
        val modifiedInput: String
    )
}