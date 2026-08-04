package com.example.pingtest

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var tvResult: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnTest: Button
    private lateinit var btnExit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        tvStatus = findViewById(R.id.tvStatus)
        btnTest = findViewById(R.id.btnTest)
        btnExit = findViewById(R.id.btnExit)

        btnTest.setOnClickListener {
            tvResult.text = "Проверка..."
            tvStatus.text = "Загрузка..."
            tvResult.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            btnTest.isEnabled = false
            btnTest.alpha = 0.6f

            val executor = Executors.newFixedThreadPool(2)
            
            val googleFuture = executor.submit { 
                checkUrl("https://www.google.com/generate_204", 5000) 
            }
            val vkFuture = executor.submit { 
                checkUrl("https://vk.com", 5000) 
            }
            
            Thread {
                try {
                    val isGoogleUp: Boolean = googleFuture.get(8, TimeUnit.SECONDS) as Boolean
                    val isVkUp: Boolean = vkFuture.get(8, TimeUnit.SECONDS) as Boolean
                    
                    Log.d("PingTest", "Google: $isGoogleUp, VK: $isVkUp")
                    
                    runOnUiThread {
                        btnTest.isEnabled = true
                        btnTest.alpha = 1f
                        executor.shutdown()
                        
                        when {
                            isGoogleUp && isVkUp -> {
                                tvResult.text = "Белые списки выкл."
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.success))
                                tvStatus.text = "Все сайты доступны"
                            }
                            !isGoogleUp && isVkUp -> {
                                tvResult.text = "Белые списки вкл."
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.error))
                                tvStatus.text = "Google заблокирован"
                            }
                            isGoogleUp && !isVkUp -> {
                                tvResult.text = "Необычная ситуация"
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                                tvStatus.text = "VK недоступен, Google работает"
                            }
                            else -> {
                                tvResult.text = "Нет интернета"
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                                tvStatus.text = "Оба сайта недоступны"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PingTest", "Timeout or error: ${e.message}")
                    runOnUiThread {
                        btnTest.isEnabled = true
                        btnTest.alpha = 1f
                        executor.shutdown()
                        tvResult.text = "Ошибка проверки"
                        tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                        tvStatus.text = "Превышено время ожидания"
                    }
                }
            }.start()
        }

        btnExit.setOnClickListener { 
            finishAffinity()
        }
    }

    private fun checkUrl(urlString: String, timeout: Int): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = "HEAD"
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            
            val code = connection.responseCode
            connection.disconnect()
            
            Log.d("PingTest", "$urlString returned $code")
            code in 200..399
        } catch (e: SocketTimeoutException) {
            Log.e("PingTest", "Timeout for $urlString")
            false
        } catch (e: Exception) {
            Log.e("PingTest", "Error checking $urlString: ${e.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }
}
