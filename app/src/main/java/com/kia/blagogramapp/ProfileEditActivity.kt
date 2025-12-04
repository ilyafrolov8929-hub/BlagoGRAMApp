package com.kia.blagogramapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            val p = getSharedPreferences("app", MODE_PRIVATE)
            p.edit().clear().apply()
            
            // Также очистим кэш
            getSharedPreferences("app_cache", MODE_PRIVATE).edit().clear().apply()

            val intent = Intent(this, StartActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun compressImage(originalFile: File): File {
        // читаем EXIF
        val exif = androidx.exifinterface.media.ExifInterface(originalFile.absolutePath)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )

        // загружаем bitmap
        var bmp = BitmapFactory.decodeFile(originalFile.absolutePath)

        // поворот если нужно
        val matrix = android.graphics.Matrix()

        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)

        // уменьшаем разрешение
        val maxSize = 800
        val ratio = bmp.width.toFloat() / bmp.height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        val resized = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true)

        // создаём сжатый файл
        val compressedFile = File(originalFile.parent, "compressed_${originalFile.name}")
        val out = FileOutputStream(compressedFile)
        resized.compress(Bitmap.CompressFormat.JPEG, 70, out)
        out.flush()
        out.close()

        return compressedFile
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
            val tmpFile = File(filesDir, fileName)

            // оригинал копируем
            contentResolver.openInputStream(uri)?.copyTo(FileOutputStream(tmpFile))

            // делаем сжатую копию
            val compressed = compressImage(tmpFile)

            // используем только сжатую версию
            avatarFile = compressed
            avatarFileName = compressed.name

            // чтобы оригинал не занимал место
            tmpFile.delete()
        }
    }

}
