package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kia.blagogramapp.api.ApiClient
import kotlinx.coroutines.launch
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_login)

        // Проверяем deep link
        val data = intent?.data
        if (data != null && data.toString() == "blagogram://register_success") {
            Toast.makeText(this, "Регистрация прошла успешно!", Toast.LENGTH_LONG).show()
        }

        val login = findViewById<EditText>(R.id.login)
        val password = findViewById<EditText>(R.id.password)
        val btn = findViewById<Button>(R.id.btnLogin)

        findViewById<TextView>(R.id.txtRegister).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://blagogram.ru/register.php?fromApp=1"
            ))
            startActivity(intent)
        }

        btn.setOnClickListener {
            val l = login.text.toString().trim()
            val p = password.text.toString().trim()

            if (l.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = ApiClient.api.login(
                        login = l,
                        password = p
                    )

                    if (!response.ok) {
                        Toast.makeText(this@LoginActivity, response.error ?: "Ошибка", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val result = response.result
                    if (result == null) {
                        Toast.makeText(this@LoginActivity, "Пустой ответ сервера", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val token = result.token
                    val userId = result.user_id
                    val username = result.username
                    val displayName = result.display_name
                    val status = result.status



                    if (token.isEmpty() || userId == null) {
                        Toast.makeText(this@LoginActivity, "Некорректный ответ сервера", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    // ✅ Сохраняем token и user_id
                    getSharedPreferences("app", MODE_PRIVATE).edit {
                        putString("token", token)
                        putInt("user_id", userId)
                        putString("avatar", result.avatar)   // ← твой параметр
                        putString("login", username)
                        putString("display_name", displayName)
                        putString("status", status)
                    }

                    startActivity(Intent(this@LoginActivity, ChatsActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
