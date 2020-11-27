package com.kimotu.url2pdf

import okhttp3.*

object HttpClient {
    val instance = OkHttpClient()
}

class HttpUtill {
    fun http_get_str(url: String): String? {
        val request = Request.Builder().url(url).build()
        val response = HttpClient.instance.newCall(request).execute()
        val body = response.body?.string()
        return body
    }

    fun http_get_byte(url: String): ByteArray? {
        val request = Request.Builder().url(url).build()
        val response = HttpClient.instance.newCall(request).execute()
        val bytes = response.body?.bytes()
        return bytes
    }
}