package com.example.zencillo_sunmi

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import com.example.zencillo_sunmi.sunmy.PrintSunmy
import com.example.zencillo_sunmi.sunmy.SunmiPrintHelper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class ZencilloSunmiPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context

    private var activityContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun getSafeContext(): Context {
        return activityContext ?: applicationContext
    }

    override fun onAttachedToEngine(
        @NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    ) {
        applicationContext = flutterPluginBinding.applicationContext

        channel = MethodChannel(
            flutterPluginBinding.binaryMessenger,
            "zencillo_sunmi"
        )

        channel.setMethodCallHandler(this)

        SunmiPrintHelper.getInstance().initSunmiPrinterService(applicationContext)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        SunmiPrintHelper.getInstance().deInitSunmiPrinterService(applicationContext)
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityContext = binding.activity
        SunmiPrintHelper.getInstance().initSunmiPrinterService(getSafeContext())
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activityContext = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activityContext = binding.activity
        SunmiPrintHelper.getInstance().initSunmiPrinterService(getSafeContext())
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
                SunmiPrintHelper.getInstance().initSunmiPrinterService(getSafeContext())
                result.success(true)
            }

            "isConnected" -> {
                result.success(SunmiPrintHelper.getInstance().isConnected())
            }

            "initPrinter" -> {
                SunmiPrintHelper.getInstance().initPrinter()
                result.success(true)
            }

            "printText" -> {
                val text = call.argument<String>("text") ?: ""
                val size = (call.argument<Number>("size") ?: 24).toFloat()
                val align = call.argument<Int>("align") ?: 1
                val bold = call.argument<Boolean>("bold") ?: false
                val underline = call.argument<Boolean>("underline") ?: false

                Thread {
                    try {
                        for (i in 0 until 1000) {
                            if (SunmiPrintHelper.getInstance().setAlign(align)) {
                                SunmiPrintHelper.getInstance().printText(
                                    text,
                                    size,
                                    bold,
                                    underline
                                )

                                mainHandler.post {
                                    result.success(true)
                                }
                                return@Thread
                            } else {
                                Thread.sleep(500)
                            }
                        }

                        mainHandler.post {
                            result.error(
                                "PRINTER_NOT_CONNECTED",
                                "La impresora Sunmi no está conectada.",
                                null
                            )
                        }
                    } catch (e: Exception) {
                        mainHandler.post {
                            result.error(
                                "PRINT_TEXT_ERROR",
                                e.message ?: "Error imprimiendo texto.",
                                null
                            )
                        }
                    }
                }.start()
            }

            "printLine" -> {
                Thread {
                    try {
                        for (i in 0 until 1000) {
                            if (SunmiPrintHelper.getInstance().setAlign(1)) {
                                SunmiPrintHelper.getInstance().printText(
                                    "--------------------------------\n",
                                    24f,
                                    false,
                                    false
                                )

                                mainHandler.post {
                                    result.success(true)
                                }
                                return@Thread
                            } else {
                                Thread.sleep(500)
                            }
                        }

                        mainHandler.post {
                            result.error(
                                "PRINTER_NOT_CONNECTED",
                                "La impresora Sunmi no está conectada.",
                                null
                            )
                        }
                    } catch (e: Exception) {
                        mainHandler.post {
                            result.error(
                                "PRINT_LINE_ERROR",
                                e.message ?: "Error imprimiendo línea.",
                                null
                            )
                        }
                    }
                }.start()
            }

            "lineWrap" -> {
                val lines = call.argument<Int>("lines") ?: 3
                SunmiPrintHelper.getInstance().lineWrap(lines)
                result.success(true)
            }

            "feedPaper" -> {
                SunmiPrintHelper.getInstance().feedPaper(getSafeContext())
                result.success(true)
            }

            "cutPaper" -> {
                SunmiPrintHelper.getInstance().cutpaper()
                result.success(true)
            }

            "printQr" -> {
                val data = call.argument<String>("data") ?: ""
                val size = call.argument<Int>("size") ?: 5
                val errorLevel = call.argument<Int>("errorLevel") ?: 0

                Thread {
                    try {
                        for (i in 0 until 1000) {
                            if (SunmiPrintHelper.getInstance().setAlign(1)) {
                                SunmiPrintHelper.getInstance().printQr(
                                    data,
                                    size,
                                    errorLevel
                                )

                                mainHandler.post {
                                    result.success(true)
                                }
                                return@Thread
                            } else {
                                Thread.sleep(500)
                            }
                        }

                        mainHandler.post {
                            result.error(
                                "PRINTER_NOT_CONNECTED",
                                "La impresora Sunmi no está conectada.",
                                null
                            )
                        }
                    } catch (e: Exception) {
                        mainHandler.post {
                            result.error(
                                "PRINT_QR_ERROR",
                                e.message ?: "Error imprimiendo QR.",
                                null
                            )
                        }
                    }
                }.start()
            }

            "printOriginal", "printSunmiOriginal" -> {
                val content = call.argument<String>("content")
                    ?: call.argument<String>("text")
                    ?: ""

                val size = call.argument<Int>("size")
                    ?: call.argument<Int>("nSize")
                    ?: 24

                SunmiPrintHelper.getInstance().initSunmiPrinterService(getSafeContext())

                PrintSunmy().initPrint(content, size) { ok, error ->
                    mainHandler.post {
                        if (ok) {
                            result.success(true)
                        } else {
                            result.error(
                                "PRINT_ORIGINAL_ERROR",
                                error ?: "Error imprimiendo en Sunmi.",
                                null
                            )
                        }
                    }
                }
            }

            else -> {
                result.notImplemented()
            }
        }
    }
}