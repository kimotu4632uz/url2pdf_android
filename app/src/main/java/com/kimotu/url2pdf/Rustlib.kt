package com.kimotu.url2pdf

object Rustlib {
    external fun geturls(input: String?): String
    external fun img2pdf(bytes: ByteArray?, pos: String?, link: String?): ByteArray

    init {
        System.loadLibrary("html2pdf")
    }
}