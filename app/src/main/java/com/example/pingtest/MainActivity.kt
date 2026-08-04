package com.example.pingtest

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var tvResult: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnTest: Button
    private lateinit var btnExit: Button

    // Зарубежные сайты (должны быть доступны без белых списков)
    private val foreignSites = listOf(
        "https://www.google.com/generate_204",
        "https://vercel.com",
        "https://github.com"
    )
    
    // Российские сайты (должны быть доступны всегда)
    private val russianSites = listOf(
        "https://vk.com",
        "https://yandex.ru",
        "https://habr.com"
    )

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
                    // Проверяем зарубежные сайты
                    val foreignAccessible = checkMultipleSites(foreignSites)
                    // Проверяем российские сайты
                    val russianAccessible = checkMultipleSites(russianSites)
                    
                    Log.d("PingTest", "Foreign: $foreignAccessible, Russian: $russianAccessible")
                    
                    runOnUiThread {
                        btnTest.isEnabled = true
                        btnTest.alpha = 1f
                        
                        when {
                            foreignAccessible && russianAccessible -> {
                                tvResult.text = "Белые списки выкл."
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.success))
                                tvStatus.text = "Все сайты доступны"
                            }
                            !foreignAccessible && russianAccessible -> {
                                tvResult.text = "Белые списки вкл."
                                tvResult.setTextColor(ContextCompat.getColor(this, R.color.error))
                                tvStatus.text = "Зарубежные сайты заблокированы"
                            }
                            foreignAccessible && !russianAccessible -> {
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
                    Log.e("PingTest", "Error: ${e.message}")
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

    private fun checkMultipleSites(sites: List<String>): Boolean {
        val executor = Executors.newFixedThreadPool(3)
        val futures = sites.map { site ->
            executor.submit {
                checkSite(site)
            }
        }
        
        var successCount = 0
        for (future in futures) {
            try {
                val result = future.get(8, TimeUnit.SECONDS) as Boolean
                if (result) successCount++
            } catch (e: Exception) {
                Log.e("PingTest", "Site check failed: ${e.message}")
            }
        }
        executor.shutdown()
        
        // Если хотя бы 2 из 3 сайтов доступны - считаем что категория работает
        return successCount >= 2
    }

    private fun checkSite(urlString: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "HEAD"
            connection.instanceFollowRedirects = true
            
            val code = connection.responseCode
            connection.disconnect()
            
            Log.d("PingTest", "$urlString -> $code")
            code in 200..399
        } catch (e: Exception) {
            Log.e("PingTest", "Failed $urlString: ${e.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }
}
