package com.example.pingtest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
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

            Thread {
                // Сначала проверяем есть ли вообще интернет
                val hasInternet = isInternetAvailable()
                Log.d("PingTest", "Internet available: $hasInternet")
                
                if (!hasInternet) {
                    runOnUiThread {
                        btnTest.isEnabled = true
                        btnTest.alpha = 1f
                        tvResult.text = "Нет интернета"
                        tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                        tvStatus.text = "Проверьте подключение"
                    }
                    return@Thread
                }

                try {
                    // Проверяем зарубежные сайты
                    val googleOk = checkSingleSite("https://www.google.com/generate_204", 8000)
                    val vercelOk = checkSingleSite("https://vercel.com", 8000)
                    val githubOk = checkSingleSite("https://github.com", 8000)
                    
                    // Проверяем российские сайты
                    val vkOk = checkSingleSite("https://vk.com", 8000)
                    val yandexOk = checkSingleSite("https://yandex.ru", 8000)
                    
                    Log.d("PingTest", "Google: $googleOk, Vercel: $vercelOk, GitHub: $githubOk")
                    Log.d("PingTest", "VK: $vkOk, Yandex: $yandexOk")
                    
                    val foreignOk = (if (googleOk) 1 else 0) + (if (vercelOk) 1 else 0) + (if (githubOk) 1 else 0)
                    val russianOk = (if (vkOk) 1 else 0) + (if (yandexOk) 1 else 0)
                    
                    runOnUiThread {
                        btnTest.isEnabled = true
                        btnTest.alpha = 1f
                        
                        when {
                            foreignOk >= 2 && russianOk >= 1 -> {
                                tvResult.text = "Белые списки выкл."
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.success))
                                tvStatus.text = "Все сайты доступны"
                            }
                            foreignOk < 2 && russianOk >= 1 -> {
                                tvResult.text = "Белые списки вкл."
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.error))
                                tvStatus.text = "Зарубежные сайты заблокированы"
                            }
                            foreignOk >= 2 && russianOk < 1 -> {
                                tvResult.text = "Необычная ситуация"
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                                tvStatus.text = "Российские сайты недоступны"
                            }
                            else -> {
                                tvResult.text = "Ошибка сети"
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                                tvStatus.text = "Проблема с подключением"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PingTest", "Error: ${e.message}", e)
                    runOnUiThread {
                        btnTest.isEnabled = true
                        btnTest.alpha = 1f
                        tvResult.text = "Ошибка проверки"
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

    private fun isInternetAvailable(): Boolean {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e("PingTest", "Network check error: ${e.message}")
            false
        }
    }

    private fun checkSingleSite(urlString: String, timeout: Int): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            connection.connect()
            
            val code = connection.responseCode
            connection.disconnect()
            
            Log.d("PingTest", "$urlString -> $code")
            code in 200..399
        } catch (e: SocketTimeoutException) {
            Log.e("PingTest", "Timeout: $urlString")
            false
        } catch (e: IOException) {
            Log.e("PingTest", "IO Error $urlString: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("PingTest", "Error $urlString: ${e.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }
}
