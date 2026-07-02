package com.example.zencillo_sunmi

import android.content.Context
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
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** ZencilloSunmiPlugin */
class ZencilloSunmiPlugin : FlutterPlugin, MethodCallHandler {

    companion object {
        private const val TAG = "ZencilloSunmiPlugin"
        private const val CHANNEL_NAME = "zencillo_sunmi"

        private const val NO_SUNMI_PRINTER = 0x00000000
        private const val CHECK_SUNMI_PRINTER = 0x00000001
        private const val FOUND_SUNMI_PRINTER = 0x00000002
        private const val LOST_SUNMI_PRINTER = 0x00000003
    }

    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    private var printerService: SunmiPrinterService? = null
    private var printerStatus: Int = CHECK_SUNMI_PRINTER
    private var isBinding: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val printerCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService?) {
            Log.d(TAG, "SUNMI onConnected service=$service")

            printerService = service
            isBinding = false

            if (service == null) {
                printerStatus = NO_SUNMI_PRINTER
                return
            }

            checkSunmiPrinterService(service)
        }

        override fun onDisconnected() {
            Log.d(TAG, "SUNMI onDisconnected")

            printerService = null
            printerStatus = LOST_SUNMI_PRINTER
            isBinding = false
        }
    }

    override fun onAttachedToEngine(
        @NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    ) {
        context = flutterPluginBinding.applicationContext

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

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "bindPrinter" -> {
                bindPrinter(result)
            }

            "isConnected" -> {
                result.success(isPrinterAvailable())
            }

            "initPrinter" -> {
                withPrinter(result) { service ->
                    service.printerInit(null)
                    setLineSpacingZero(service)
                    result.success(true)
                }
            }

            "printText" -> {
                val text = call.argument<String>("text") ?: ""
                val size = (call.argument<Number>("size") ?: 24).toFloat()
                val align = call.argument<Int>("align") ?: 0
                val bold = call.argument<Boolean>("bold") ?: false

                withPrinter(result) { service ->
                    service.setAlignment(align, null)
                    setLineSpacingZero(service)
                    setBold(service, bold)

                    service.printTextWithFont(
                        text.trimEnd('\n', '\r'),
                        null,
                        size,
                        null
                    )

                    service.lineWrap(1, null)

                    if (bold) {
                        setBold(service, false)
                    }

                    result.success(true)
                }
            }

            "printLine" -> {
                withPrinter(result) { service ->
                    setLineSpacingZero(service)
                    service.printText("--------------------------------", null)
                    service.lineWrap(1, null)
                    result.success(true)
                }
            }

            "lineWrap" -> {
                val lines = call.argument<Int>("lines") ?: 3

                withPrinter(result) { service ->
                    service.lineWrap(lines, null)
                    result.success(true)
                }
            }

            "feedPaper" -> {
                val lines = call.argument<Int>("lines") ?: 8

                withPrinter(result) { service ->
                    try {
                        service.autoOutPaper(null)
                    } catch (e: RemoteException) {
                        service.lineWrap(lines, null)
                    }

                    result.success(true)
                }
            }

            "cutPaper" -> {
                withPrinter(result) { service ->
                    try {
                        service.cutPaper(null)
                    } catch (e: Exception) {
                        Log.e(TAG, "SUNMI cutPaper error", e)
                    }

                    result.success(true)
                }
            }

            "printQr" -> {
                val data = call.argument<String>("data") ?: ""
                val size = call.argument<Int>("size") ?: 6
                val errorLevel = call.argument<Int>("errorLevel") ?: 2

                if (data.isBlank()) {
                    result.error(
                        "INVALID_QR",
                        "El contenido del QR está vacío.",
                        null
                    )
                    return
                }

                withPrinter(result) { service ->
                    service.setAlignment(1, null)
                    setLineSpacingZero(service)

                    service.printQRCode(
                        data,
                        size,
                        errorLevel,
                        null
                    )

                    service.lineWrap(1, null)

                    result.success(true)
                }
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    /**
     * Implementación oficial SUNMI:
     * InnerPrinterManager.getInstance().bindService(context, printerCallback)
     */
    private fun initSunmiPrinterService(): Boolean {
        if (printerService != null) {
            return true
        }

        if (isBinding) {
            return true
        }

        return try {
            isBinding = true
            printerStatus = CHECK_SUNMI_PRINTER

            val started = InnerPrinterManager.getInstance()
                .bindService(context, printerCallback)

            Log.d(TAG, "SUNMI bindService started=$started")

            if (!started) {
                isBinding = false
                printerStatus = NO_SUNMI_PRINTER
            }

            started
        } catch (e: InnerPrinterException) {
            isBinding = false
            printerStatus = NO_SUNMI_PRINTER
            Log.e(TAG, "SUNMI bindService InnerPrinterException", e)
            false
        } catch (e: Exception) {
            isBinding = false
            printerStatus = NO_SUNMI_PRINTER
            Log.e(TAG, "SUNMI bindService Exception", e)
            false
        }
    }

    /**
     * Implementación oficial SUNMI:
     * InnerPrinterManager.getInstance().unBindService(context, printerCallback)
     */
    private fun deInitSunmiPrinterService() {
        try {
            InnerPrinterManager.getInstance()
                .unBindService(context, printerCallback)
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI unBindService error", e)
        } finally {
            printerService = null
            printerStatus = LOST_SUNMI_PRINTER
            isBinding = false
        }
    }

    /**
     * Patrón del demo oficial:
     * validar si el service realmente tiene impresora.
     */
    private fun checkSunmiPrinterService(service: SunmiPrinterService) {
        val hasPrinter = try {
            InnerPrinterManager.getInstance().hasPrinter(service)
        } catch (e: InnerPrinterException) {
            Log.e(TAG, "SUNMI hasPrinter InnerPrinterException", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI hasPrinter Exception", e)
            false
        }

        printerStatus = if (hasPrinter) {
            FOUND_SUNMI_PRINTER
        } else {
            NO_SUNMI_PRINTER
        }

        Log.d(TAG, "SUNMI hasPrinter=$hasPrinter status=$printerStatus")
    }

    private fun bindPrinter(result: Result) {
        if (isPrinterAvailable()) {
            result.success(true)
            return
        }

        val started = initSunmiPrinterService()

        if (!started && printerService == null) {
            result.success(false)
            return
        }

        waitForPrinter(
            maxAttempts = 40,
            delayMs = 150L,
            onReady = {
                result.success(true)
            },
            onTimeout = {
                result.success(false)
            }
        )
    }

    private fun isPrinterAvailable(): Boolean {
        val service = printerService ?: return false

        return try {
            InnerPrinterManager.getInstance().hasPrinter(service)
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI isPrinterAvailable error", e)

            // Algunos modelos viejos fallan en hasPrinter aunque imprimen.
            // Si quieres ser 100% estricto con la documentación oficial, cambia esto a false.
            true
        }
    }

    private fun withPrinter(
        result: Result,
        action: (SunmiPrinterService) -> Unit
    ) {
        val currentService = printerService

        if (currentService != null) {
            try {
                action(currentService)
            } catch (e: RemoteException) {
                Log.e(TAG, "SUNMI RemoteException", e)
                result.error(
                    "SUNMI_REMOTE_ERROR",
                    e.message ?: "Error remoto de impresora Sunmi.",
                    null
                )
            } catch (e: Exception) {
                Log.e(TAG, "SUNMI Exception", e)
                result.error(
                    "SUNMI_ERROR",
                    e.message ?: "Error ejecutando operación Sunmi.",
                    null
                )
            }
            return
        }

        val started = initSunmiPrinterService()

        if (!started && printerService == null) {
            result.error(
                "PRINTER_BIND_FAILED",
                "No se pudo enlazar con el servicio de impresora Sunmi.",
                null
            )
            return
        }

        waitForPrinter(
            maxAttempts = 40,
            delayMs = 150L,
            onReady = {
                val readyService = printerService

                if (readyService == null) {
                    result.error(
                        "PRINTER_NOT_CONNECTED",
                        "La impresora Sunmi no está conectada.",
                        null
                    )
                    return@waitForPrinter
                }

                try {
                    action(readyService)
                } catch (e: RemoteException) {
                    Log.e(TAG, "SUNMI RemoteException", e)
                    result.error(
                        "SUNMI_REMOTE_ERROR",
                        e.message ?: "Error remoto de impresora Sunmi.",
                        null
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "SUNMI Exception", e)
                    result.error(
                        "SUNMI_ERROR",
                        e.message ?: "Error ejecutando operación Sunmi.",
                        null
                    )
                }
            },
            onTimeout = {
                result.error(
                    "PRINTER_NOT_CONNECTED",
                    "La impresora Sunmi no está conectada.",
                    null
                )
            }
        )
    }

    private fun waitForPrinter(
        maxAttempts: Int,
        delayMs: Long,
        attempt: Int = 0,
        onReady: () -> Unit,
        onTimeout: () -> Unit
    ) {
        val service = printerService

        if (service != null) {
            try {
                checkSunmiPrinterService(service)
            } catch (e: Exception) {
                Log.e(TAG, "SUNMI check during wait error", e)
            }

            onReady()
            return
        }

        if (attempt >= maxAttempts) {
            onTimeout()
            return
        }

        mainHandler.postDelayed({
            waitForPrinter(
                maxAttempts = maxAttempts,
                delayMs = delayMs,
                attempt = attempt + 1,
                onReady = onReady,
                onTimeout = onTimeout
            )
        }, delayMs)
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
}