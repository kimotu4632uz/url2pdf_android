package com.kimotu.url2pdf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.lang.Exception
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), CoroutineScope {
    val CREATE_FILE = 2002
    var pdf = byteArrayOf()

    private val supervisor = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + supervisor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        url_enter.setOnClickListener {
            val orig_url = url_fill.text.toString()
            logcat.append("Use url: " + orig_url + "\n")
            getUrls(orig_url)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supervisor.cancelChildren()
    }

    fun getUrls(url: String) = launch {
        val http = HttpUtill()

        logcat.append("Send HTTP request to get html...\n")
        val html = async(Dispatchers.IO) { http.http_get_str(url) }.await()
        logcat.append("Received html\n")

        logcat.append("Start parsing html\n")
        val urls = Rustlib.geturls(html).lines()
        logcat.append("Got " + urls.size + " url\n")

        logcat.append("Send multiple HTTP request to get images...\n")
        val resps = urls.map{url -> async(Dispatchers.IO) { Pair(url, http.http_get_byte(url) ?: byteArrayOf()) }}.awaitAll().toMap()
        logcat.append("Finished getting images\n")

        var bytes = byteArrayOf()
        var sizes = ""
        for (key in urls) {
            bytes += resps[key] ?: byteArrayOf()
            sizes += resps[key]?.size ?: ""
            sizes += "\n"
        }

        logcat.append("Start generating pdf from imgaes...\n")
        pdf = Rustlib.img2pdf(bytes, sizes, url)
        logcat.append("Finished generating pdf\n")

        runOnUiThread {
            val create_file_intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                setType("application/pdf")
                putExtra(Intent.EXTRA_TITLE, "out.pdf")
            }

            logcat.append("Send Intent to save pdf\n")
            startActivityForResult(create_file_intent, CREATE_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            logcat.append("Intent received")

            val uri = data?.data ?: run {
                logcat.append("Error: could not get content uri from Intent\n")
                return
            }

            try {
                logcat.append("Writing pdf to uri...\n")
                contentResolver.openOutputStream(uri)?.write(pdf)
            } catch (e: Exception) {
                logcat.append("Error: Could not write pdf\n" + e.stackTraceToString())
            }

            logcat.append("Finished all !\n")
        }
    }
}