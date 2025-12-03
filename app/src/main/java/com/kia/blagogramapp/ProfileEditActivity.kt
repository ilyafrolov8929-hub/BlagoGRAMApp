package com.kia.blagogramapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileEditActivity : AppCompatActivity() {

    private val PICK_AVATAR = 2001

    private lateinit var inputName: EditText
    private lateinit var inputStatus: EditText
    private lateinit var avatarView: ShapeableImageView

    private var avatarFile: File? = null
    private var avatarFileName: String? = null

    private lateinit var repo: ApiRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

        inputName = findViewById(R.id.editName)
        inputStatus = findViewById(R.id.editStatus)
        avatarView = findViewById(R.id.editAvatar)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)

        inputName.setText(prefs.getString("display_name", ""))
        inputStatus.setText(prefs.getString("status", ""))

        avatarView.load("https://blagogram.ru/avatars/${prefs.getString("avatar", "")}")

        findViewById<ImageView>(R.id.editAvatarBtn).setOnClickListener {
            val pick = Intent(Intent.ACTION_PICK)
            pick.type = "image/*"
            startActivityForResult(pick, PICK_AVATAR)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveProfile()
        }

        findViewById<ImageView>(R.id.btnBackProfileEdit).setOnClickListener {
            finish()
        }
    }

    // ---- Сохранение профиля ----
    private fun saveProfile() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)

        val token = prefs.getString("token", "") ?: ""
        val name = inputName.text.toString().trim()
        val status = inputStatus.text.toString().trim()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val avatarLocal = avatarFile

                val response = if (avatarLocal != null) {
                    val req = avatarLocal.asRequestBody("image/jpeg".toMediaType())
                    val part = MultipartBody.Part.createFormData(
                        "avatar",
                        avatarLocal.name,
                        req
                    )

                    repo.profileEditMultipart(
                        token, name, status, part
                    )
                } else {
                    repo.profileEdit(
                        token, name, status
                    )
                }

                if (response["ok"] == true) {

                    val result = response["result"] as Map<*, *>

                    prefs.edit()
                        .putString("display_name", name)
                        .putString("status", status)
                        .apply()

                    val avatarFromServer = result["avatar"]?.toString()
                    if (avatarFromServer != null)
                        prefs.edit().putString("avatar", avatarFromServer).apply()

                    runOnUiThread {
                        Toast.makeText(this@ProfileEditActivity, "Сохранено!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileEditActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ---- Выбор аватара ----
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_AVATAR && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return

            avatarView.load(uri)

            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val userId = prefs.getInt("user_id", 0)

            val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)

            contentResolver.openInputStream(uri)?.copyTo(FileOutputStream(file))

            avatarFile = file
            avatarFileName = fileName
        }
    }
}
