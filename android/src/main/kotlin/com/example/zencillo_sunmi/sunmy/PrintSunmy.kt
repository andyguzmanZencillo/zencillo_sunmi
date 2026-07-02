package com.example.zencillo_sunmi.sunmy

class PrintSunmy {

    fun initPrint(
        sContent: String,
        nSize: Int,
        callback: ((Boolean, String?) -> Unit)? = null
    ) {
        val r = Runnable {
            try {
                var printed = false

                for (i in 0 until 1000) {
                    if (SunmiPrintHelper.getInstance().setAlign(1)) {
                        for (j in 1 until 10) {
                            SunmiPrintHelper.getInstance().setAlign(1)
                        }

                        val result = ResultPrint()
                        result.extractAndRemoveQrCodeUrl(sContent)

                        if (result.extractedCodeQR != null) {
                            SunmiPrintHelper.getInstance().printText(
                                result.modifiedInput,
                                nSize.toFloat(),
                                true,
                                false
                            )

                            SunmiPrintHelper.getInstance().printText(
                                "  \n",
                                nSize.toFloat(),
                                false,
                                false
                            )

                            SunmiPrintHelper.getInstance().printQr(
                                result.extractedCodeQR!!,
                                5,
                                0
                            )
                        } else {
                            SunmiPrintHelper.getInstance().printText(
                                sContent,
                                nSize.toFloat(),
                                true,
                                false
                            )
                        }

                        for (x in 1..6) {
                            SunmiPrintHelper.getInstance().printText(
                                "  \n",
                                nSize.toFloat(),
                                false,
                                false
                            )
                        }

                        printed = true
                        break
                    } else {
                        Thread.sleep(500)
                    }
                }

                if (printed) {
                    callback?.invoke(true, null)
                } else {
                    callback?.invoke(
                        false,
                        "No se pudo imprimir: la impresora Sunmi no respondió."
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                callback?.invoke(false, ex.message)
            }
        }

        val t = Thread(r)
        t.start()
    }
}