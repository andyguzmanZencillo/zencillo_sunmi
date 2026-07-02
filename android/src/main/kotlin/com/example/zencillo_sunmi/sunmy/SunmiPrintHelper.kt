package com.example.zencillo_sunmi.sunmy

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.sunmi.peripheral.printer.WoyouConsts

class SunmiPrintHelper private constructor() {

    companion object {
        private const val TAG = "SunmiPrintHelper"

        const val NoSunmiPrinter = 0x00000000
        const val CheckSunmiPrinter = 0x00000001
        const val FoundSunmiPrinter = 0x00000002
        const val LostSunmiPrinter = 0x00000003

        private val helper = SunmiPrintHelper()

        fun getInstance(): SunmiPrintHelper {
            return helper
        }
    }

    var sunmiPrinter: Int = CheckSunmiPrinter
        private set

    private var sunmiPrinterService: SunmiPrinterService? = null
    private var isBinding: Boolean = false

    private val innerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService?) {
            Log.d(TAG, "onConnected service=$service")

            sunmiPrinterService = service
            isBinding = false

            if (service != null) {
                checkSunmiPrinterService(service)
            } else {
                sunmiPrinter = NoSunmiPrinter
            }
        }

        override fun onDisconnected() {
            Log.d(TAG, "onDisconnected")

            sunmiPrinterService = null
            sunmiPrinter = LostSunmiPrinter
            isBinding = false
        }
    }

    fun initSunmiPrinterService(context: Context) {
        try {
            if (sunmiPrinterService != null) {
                return
            }

            if (isBinding) {
                return
            }

            isBinding = true

            var ret = false

            for (nIntento in 0..50) {
                ret = InnerPrinterManager.getInstance().bindService(
                    context,
                    innerPrinterCallback
                )

                Log.d(TAG, "bindService intento=$nIntento ret=$ret")

                if (ret) {
                    break
                }
            }

            if (!ret) {
                isBinding = false
                sunmiPrinter = NoSunmiPrinter
            }
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
            isBinding = false
            sunmiPrinter = NoSunmiPrinter
        } catch (e: Exception) {
            e.printStackTrace()
            isBinding = false
            sunmiPrinter = NoSunmiPrinter
        }
    }

    fun deInitSunmiPrinterService(context: Context) {
        try {
            if (sunmiPrinterService != null) {
                InnerPrinterManager.getInstance().unBindService(
                    context,
                    innerPrinterCallback
                )

                sunmiPrinterService = null
                sunmiPrinter = LostSunmiPrinter
            }
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isBinding = false
        }
    }

    private fun checkSunmiPrinterService(service: SunmiPrinterService) {
        var ret = false

        try {
            ret = InnerPrinterManager.getInstance().hasPrinter(service)
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        sunmiPrinter = if (ret) {
            FoundSunmiPrinter
        } else {
            NoSunmiPrinter
        }

        Log.d(TAG, "checkSunmiPrinterService ret=$ret sunmiPrinter=$sunmiPrinter")
    }

    private fun handleRemoteException(e: RemoteException) {
        e.printStackTrace()
    }

    fun isConnected(): Boolean {
        return sunmiPrinterService != null
    }

    fun sendRawData(data: ByteArray) {
        val service = sunmiPrinterService ?: return

        try {
            service.sendRAWData(data, null)
        } catch (e: RemoteException) {
            handleRemoteException(e)
        }
    }

    fun cutpaper() {
        val service = sunmiPrinterService ?: return

        try {
            service.cutPaper(null)
        } catch (e: RemoteException) {
            handleRemoteException(e)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initPrinter() {
        val service = sunmiPrinterService ?: return

        try {
            service.printerInit(null)
        } catch (e: RemoteException) {
            handleRemoteException(e)
        }
    }

    fun print3Line() {
        val service = sunmiPrinterService ?: return

        try {
            service.lineWrap(3, null)
        } catch (e: RemoteException) {
            handleRemoteException(e)
        }
    }

    fun lineWrap(lines: Int) {
        val service = sunmiPrinterService ?: return

        try {
            service.lineWrap(lines, null)
        } catch (e: RemoteException) {
            handleRemoteException(e)
        }
    }

    fun getPrinterSerialNo(): String {
        val service = sunmiPrinterService ?: return ""

        return try {
            service.printerSerialNo
        } catch (e: RemoteException) {
            handleRemoteException(e)
            ""
        }
    }

    fun getDeviceModel(): String {
        val service = sunmiPrinterService ?: return ""

        return try {
            service.printerModal
        } catch (e: RemoteException) {
            handleRemoteException(e)
            ""
        }
    }

    fun getPrinterVersion(): String {
        val service = sunmiPrinterService ?: return ""

        return try {
            service.printerVersion
        } catch (e: RemoteException) {
            handleRemoteException(e)
            ""
        }
    }

    fun getPrinterPaper(): String {
        val service = sunmiPrinterService ?: return ""

        return try {
            if (service.printerPaper == 1) {
                "58mm"
            } else {
                "80mm"
            }
        } catch (e: RemoteException) {
            handleRemoteException(e)
            ""
        }
    }

    fun setAlign(align: Int): Boolean {
        val service = sunmiPrinterService

        if (service == null) {
            Log.d(TAG, "NO LOGRO ALINEAR")
            return false
        }

        return try {
            Log.d(TAG, "SI LOGRO ALINEAR")
            service.setAlignment(align, null)
            true
        } catch (e: RemoteException) {
            Log.d(TAG, "SI NO LOGRO ALINEAR")
            handleRemoteException(e)
            false
        } catch (e: Exception) {
            Log.d(TAG, "SI NO LOGRO ALINEAR")
            false
        }
    }

    fun feedPaper(context: Context) {
        val service = sunmiPrinterService ?: return

        try {
            service.autoOutPaper(null)
        } catch (e: RemoteException) {
            print3Line()
        } catch (e: Exception) {
            print3Line()
        }
    }

    fun printText(
        content: String,
        size: Float,
        isBold: Boolean,
        isUnderLine: Boolean
    ) {
        val service = sunmiPrinterService ?: return

        try {
            try {
                service.setPrinterStyle(WoyouConsts.SET_LINE_SPACING, 0)
            } catch (e: RemoteException) {
                service.sendRAWData(byteArrayOf(0x1B, 0x33, 0x00), null)
            }

            try {
                service.setPrinterStyle(
                    WoyouConsts.ENABLE_BOLD,
                    if (isBold) WoyouConsts.ENABLE else WoyouConsts.DISABLE
                )
            } catch (e: RemoteException) {
                if (isBold) {
                    service.sendRAWData(ESCUtil.boldOn(), null)
                } else {
                    service.sendRAWData(ESCUtil.boldOff(), null)
                }
            }

            try {
                service.setPrinterStyle(
                    WoyouConsts.ENABLE_UNDERLINE,
                    if (isUnderLine) WoyouConsts.ENABLE else WoyouConsts.DISABLE
                )
            } catch (e: RemoteException) {
                if (isUnderLine) {
                    service.sendRAWData(ESCUtil.underlineWithOneDotWidthOn(), null)
                } else {
                    service.sendRAWData(ESCUtil.underlineOff(), null)
                }
            }

            service.printTextWithFont(content, null, size, null)
        } catch (e: RemoteException) {
            handleRemoteException(e)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun printQr(
        data: String,
        modulesize: Int,
        errorlevel: Int
    ) {
        val service = sunmiPrinterService ?: return

        try {
            service.printQRCode(data, modulesize, errorlevel, null)
        } catch (e: RemoteException) {
            handleRemoteException(e)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}