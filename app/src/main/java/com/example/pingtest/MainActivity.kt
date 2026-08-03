package com.example.pingtest

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL

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

        // Анимация появления
        tvResult.alpha = 0f
        tvResult.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()

        btnTest.setOnClickListener {
            tvResult.text = "Проверка..."
            tvStatus.text = "⏳ Загрузка..."
            tvResult.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            btnTest.isEnabled = false
            btnTest.alpha = 0.6f

            Thread {
                val isGoogleUp = checkUrl("https://www.google.com/generate_204")
                val isVkUp = checkUrl("https://vk.com")
                Log.d("PingTest", "Google: $isGoogleUp, VK: $isVkUp")
                
                runOnUiThread {
                    btnTest.isEnabled = true
                    btnTest.alpha = 1f
                    
                    when {
                        isGoogleUp && isVkUp -> {
                            tvResult.text = "✅ Белые списки выкл."
                            tvResult.setTextColor(ContextCompat.getColor(this, R.color.success))
                            tvStatus.text = "🟢 Все сайты доступны"
                            showSuccessAnimation()
                        }
                        !isGoogleUp && isVkUp -> {
                            tvResult.text = "🚫 Белые списки вкл."
                            tvResult.setTextColor(ContextCompat.getColor(this, R.color.error))
                            tvStatus.text = "🔴 Google заблокирован"
                            showErrorAnimation()
                        }
                        else -> {
                            tvResult.text = "❌ Нет интернета"
                            tvResult.setTextColor(ContextCompat.getColor(this, R.color.warning))
                            tvStatus.text = "⚠️ Проверьте подключение"
                        }
                    }
                }
            }.start()
        }

        btnExit.setOnClickListener { 
            finishAffinity()
        }
    }

    private fun showSuccessAnimation() {
        val animator = ObjectAnimator.ofFloat(tvResult, "scaleX", 1f, 1.2f, 1f)
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun showErrorAnimation() {
        val animator = ObjectAnimator.ofFloat(tvResult, "translationX", 0f, -20f, 20f, -20f, 20f, 0f)
        animator.duration = 500
        animator.start()
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
