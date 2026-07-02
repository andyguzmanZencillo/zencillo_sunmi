package com.example.zencillo_sunmi.sunmy

object ESCUtil {

    private const val ESC: Byte = 0x1B

    fun boldOn(): ByteArray {
        return byteArrayOf(ESC, 69, 1)
    }

    fun boldOff(): ByteArray {
        return byteArrayOf(ESC, 69, 0)
    }

    fun underlineWithOneDotWidthOn(): ByteArray {
        return byteArrayOf(ESC, 45, 1)
    }

    fun underlineOff(): ByteArray {
        return byteArrayOf(ESC, 45, 0)
    }
}