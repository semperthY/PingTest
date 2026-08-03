package com.example.pingtest

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var tvResult: TextView
    private lateinit var btnTest: Button
    private lateinit var btnExit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        btnTest = findViewById(R.id.btnTest)
        btnExit = findViewById(R.id.btnExit)

        btnTest.setOnClickListener {
            tvResult.text = "Проверка... (подождите 10 сек)"
            btnTest.isEnabled = false
            Thread {
                val isGoogleUp = checkUrl("https://www.google.com/generate_204")
                val isVkUp = checkUrl("https://vk.com")
                Log.d("PingTest", "Google: $isGoogleUp, VK: $isVkUp")
                runOnUiThread {
                    btnTest.isEnabled = true
                    when {
                        isGoogleUp && isVkUp -> {
                            tvResult.text = "Белые списки выкл."
                            tvResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                        }
                        !isGoogleUp && isVkUp -> {
                            tvResult.text = "Белые списки вкл."
                            tvResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                        }
                        else -> {
                            tvResult.text = "Нет интернета или ошибка"
                            tvResult.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                        }
                    }
                }
            }.start()
        }

        btnExit.setOnClickListener { finishAffinity() }
    }

    private fun checkUrl(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            val code = connection.responseCode
            connection.disconnect()
            code in 200..399
        } catch (e: Exception) {
            Log.e("PingTest", "Error checking $urlString: ${e.message}")
            false
        }
    }
}
