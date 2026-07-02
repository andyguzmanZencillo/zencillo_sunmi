package com.example.zencillo_sunmi.sunmy

import java.util.regex.Pattern

class ResultPrint {

    var extractedCodeQR: String? = null
    var modifiedInput: String = ""

    fun extractAndRemoveQrCodeUrl(inputValue: String) {
        val pattern = "!-QR-!(.*?)!-QR-!"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(inputValue)

        var input = inputValue
        var extractedUrl: String? = null

        if (matcher.find()) {
            extractedUrl = matcher.group(1)
            input = matcher.replaceFirst("")
        }

        extractedCodeQR = extractedUrl
        modifiedInput = input
    }
}