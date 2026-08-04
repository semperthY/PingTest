package com.example.pingtest

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.InetAddress
import java.net.SocketTimeoutException
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

            Thread {
                try {
                    // Проверяем через DNS + ping - самый надежный метод
                    val googleOk = pingHost("google.com", 5000)
                    val vercelOk = pingHost("vercel.com", 5000)
                    val githubOk = pingHost("github.com", 5000)
                    
                    val vkOk = pingHost("vk.com", 5000)
                    val yandexOk = pingHost("yandex.ru", 5000)
                    
                    Log.d("PingTest", "Google: $googleOk, Vercel: $vercelOk, GitHub: $githubOk")
                    Log.d("PingTest", "VK: $vkOk, Yandex: $yandexOk")
                    
                    val foreignCount = (if (googleOk) 1 else 0) + (if (vercelOk) 1 else 0) + (if (githubOk) 1 else 0)
                    val russianCount = (if (vkOk) 1 else 0) + (if (yandexOk) 1 else 0)
                    
                    runOnUiThread {
                        btnTest.isEnabled = true
                        btnTest.alpha = 1f
                        
                        when {
                            foreignCount >= 1 && russianCount >= 1 -> {
                                tvResult.text = "Белые списки выкл."
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.success))
                                tvStatus.text = "Все сайты доступны"
                            }
                            foreignCount == 0 && russianCount >= 1 -> {
                                tvResult.text = "Белые списки вкл."
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.error))
                                tvStatus.text = "Зарубежные сайты заблокированы"
                            }
                            foreignCount >= 1 && russianCount == 0 -> {
                                tvResult.text = "Необычная ситуация"
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                                tvStatus.text = "Российские сайты недоступны"
                            }
                            else -> {
                                tvResult.text = "Нет интернета"
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                                tvStatus.text = "Все сайты недоступны"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PingTest", "Fatal error: ${e.message}", e)
                    runOnUiThread {
                        btnTest.isEnabled = true
                        btnTest.alpha = 1f
                        tvResult.text = "Ошибка"
                        tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                        tvStatus.text = "Попробуйте еще раз"
                    }
                }
            }.start()
        }

        btnExit.setOnClickListener { 
            finishAffinity()
        }
    }

    private fun pingHost(host: String, timeout: Int): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            Log.d("PingTest", "Pinging $host (${address.hostAddress})")
            address.isReachable(timeout)
        } catch (e: SocketTimeoutException) {
            Log.e("PingTest", "Ping timeout: $host")
            false
        } catch (e: Exception) {
            Log.e("PingTest", "Ping error $host: ${e.message}")
            false
        }
    }
}
