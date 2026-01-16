package com.kia.blagogramapp

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import coil.load
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.model.Post
import com.kia.blagogramapp.ui.modules.BottomNavigationModule
import com.kia.blagogramapp.ui.modules.MediaPickerSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import androidx.core.view.setPadding
import android.net.Uri
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.database.AppDatabase
import com.kia.blagogramapp.database.PostEntity
import com.kia.blagogramapp.database.UserEntity
import com.kia.blagogramapp.ui.modules.PostCreationModule
import com.kia.blagogramapp.ui.modules.ShareModule

class NewsActivity : AppCompatActivity() {
    private lateinit var root: FrameLayout
    private lateinit var newsContainer: LinearLayout
    private lateinit var postCreationOverlay: FrameLayout
    
    private lateinit var apiRepo: ApiRepository
    private var selectedImageFile: File? = null
    private lateinit var overlayImagePreview: ShapeableImageView
    private lateinit var overlayImageContainer: FrameLayout
    
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val userCache = mutableMapOf<Int, UserEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        apiRepo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

        root = DesignEngine.initializeScreenWithFrameLayout(this)
        setContentView(root)

        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDark = themePrefs.getBoolean("is_dark_theme", false)

        val scrollView = NestedScrollView(this).apply {
            isFillViewport = true
        }
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0, 0, 0, 0, 0))

        val mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(100), LayoutHelper.dp(16), LayoutHelper.dp(16))
        }
        scrollView.addView(mainContent)

        val currentUserId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        val isOwner = currentUserId == 1

        val toolbar = if (isOwner) {
            DesignEngine.createToolBar(
                context = this,
                title = "Новости",
                leftButtonIconRes = R.drawable.ic_back,
                rightButtonIconRes = R.drawable.ic_add,
                rightButtonClickListener = { PostCreationModule.show(this) { loadPosts() } },
                isDark = isDark,
                leftButtonClickListener = { finish() }
            )
        } else {
            DesignEngine.createToolBar(
                context = this,
                title = "Новости",
                leftButtonIconRes = R.drawable.ic_back,
                isDark = isDark,
                leftButtonClickListener = { finish() }
            )
        }

        root.addView(toolbar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        newsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        mainContent.addView(newsContainer)

        if (DesignEngine.useBottomNavigation) {
            BottomNavigationModule(this).create(root, BottomNavigationModule.Screen.MENU)
            mainContent.setPadding(mainContent.paddingLeft, mainContent.paddingTop, mainContent.paddingRight, LayoutHelper.dp(100))
        }
        
        setupPostCreationOverlay()
        
        loadCachedPosts()
        loadPosts()
    }

    private fun loadCachedPosts() {
        CoroutineScope(Dispatchers.Main).launch {
            val cached = withContext(Dispatchers.IO) { db.postDao().getAll() }
            if (cached.isNotEmpty()) {
                displayPosts(cached.map { Post(it.id, it.authorId, it.title, it.content, it.image, it.createdAt, it.likes, it.liked) })
            }
        }
    }

    private fun displayPosts(posts: List<Post>) {
        newsContainer.removeAllViews()
        posts.forEach { post ->
            val cell = PostCell(this@NewsActivity)
            cell.timeView.text = post.createdAt.split(" ").lastOrNull()?.take(5) ?: ""
            cell.contentText.text = post.content
            
            // Загружаем инфо о пользователе
            loadUserInfo(post.authorId, cell)
            
            if (!post.image.isNullOrEmpty()) {
                cell.postImage.visibility = View.VISIBLE
                cell.postImage.load("https://blagogram.ru/posts/${post.image}")
            }
            
            cell.setLiked(post.liked)
            updateLikesCount(cell, post.likes, post.liked)
            
            cell.likeButton.setOnClickListener {
                toggleLike(post, cell)
            }
            
            cell.sharePill.setOnClickListener {
                ShareModule.show(this, post)
            }
            
            newsContainer.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 12))
        }
    }

    private fun loadUserInfo(userId: Int, cell: PostCell) {
        // 1. Проверяем RAM кэш
        userCache[userId]?.let {
            cell.setUserInfo(it.displayName ?: it.username, it.avatar)
            return
        }

        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        
        CoroutineScope(Dispatchers.Main).launch {
            // 2. Проверяем БД
            val cachedUser = withContext(Dispatchers.IO) { db.userDao().getById(userId) }
            if (cachedUser != null && (System.currentTimeMillis() - cachedUser.lastUpdated < 3600000)) { // Кэш на 1 час
                userCache[userId] = cachedUser
                cell.setUserInfo(cachedUser.displayName ?: cachedUser.username, cachedUser.avatar)
            } else {
                // 3. Идем в сеть
                try {
                    Toast.makeText(this@NewsActivity, userId.toString(), Toast.LENGTH_SHORT).show()
                    val profile = withContext(Dispatchers.IO) { apiRepo.getUserProfile(token, userId) }
                    val userEntity = UserEntity(
                        id = profile.id,
                        username = profile.username,
                        displayName = profile.display_name,
                        avatar = profile.avatar,
                        lastUpdated = System.currentTimeMillis()
                    )
                    withContext(Dispatchers.IO) { db.userDao().insert(userEntity) }
                    userCache[userId] = userEntity
                    cell.setUserInfo(userEntity.displayName ?: userEntity.username, userEntity.avatar)
                } catch (e: Exception) {
                    cell.setUserInfo("Пользователь #$userId", null)
                }
            }
        }
    }

    private fun loadPosts() {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val posts = withContext(Dispatchers.IO) { apiRepo.getPosts(token) }
                displayPosts(posts)
                
                withContext(Dispatchers.IO) {
                    db.postDao().deleteAll()
                    db.postDao().insertAll(posts.map { PostEntity(it.id, it.authorId, it.title, it.content, it.image, it.createdAt, it.likes, it.liked) })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateLikesCount(cell: PostCell, count: Int, isLiked: Boolean) {
        cell.likesCount.text = if (count > 0) count.toString() else ""
        cell.likesCount.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun toggleLike(post: Post, cell: PostCell) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val liked = withContext(Dispatchers.IO) { apiRepo.likePost(token, post.id) }
                cell.setLiked(liked)
                
                val currentLikes = post.likes
                val newLikes = if (liked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
                updateLikesCount(cell, newLikes, liked)
                
                withContext(Dispatchers.IO) {
                    val entity = PostEntity(post.id, post.authorId, post.title, post.content, post.image, post.createdAt, newLikes, liked)
                    db.postDao().update(entity)
                }
                
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupPostCreationOverlay() {
        val isDark = getSharedPreferences("app_theme", MODE_PRIVATE).getBoolean("is_dark_theme", false)
        
        postCreationOverlay = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(DesignEngine.Theme.colorBackground)
            isClickable = true
            isFocusable = true
        }
        
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        postCreationOverlay.addView(content)

        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Создание поста",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = isDark,
            leftButtonClickListener = { hidePostCreationOverlay() }
        )
        content.addView(toolbar)
        
        val scroll = NestedScrollView(this).apply { isFillViewport = true }
        val innerContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16))
        }
        scroll.addView(innerContent)
        content.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))

        val headerBox = RelativeLayout(this)
        val photoBtn = TextView(this).apply {
            text = "Фото"
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(18).toFloat()
            }
            setOnClickListener { selectPostImage() }
        }
        headerBox.addView(photoBtn, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply { 
            addRule(RelativeLayout.ALIGN_PARENT_END)
        })
        innerContent.addView(headerBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        val input = EditText(this).apply {
            hint = "Текст..."
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.TOP
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            setPadding(LayoutHelper.dp(16))
            minHeight = LayoutHelper.dp(200)
        }
        innerContent.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        overlayImageContainer = FrameLayout(this).apply {
            visibility = View.GONE
            setPadding(0, 0, 0, LayoutHelper.dp(16))
        }
        overlayImagePreview = ShapeableImageView(this).apply {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(12).toFloat()).build()
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        overlayImageContainer.addView(overlayImagePreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200))
        
        val removeImg = ImageView(this).apply {
            setImageResource(R.drawable.ic_close_small)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x80000000.toInt()) }
            setOnClickListener { 
                selectedImageFile = null
                overlayImageContainer.visibility = View.GONE
            }
        }
        overlayImageContainer.addView(removeImg, LayoutHelper.createFrame(24, 24, Gravity.TOP or Gravity.END, 0, 8, 8, 0))
        innerContent.addView(overlayImageContainer)

        val publishBtn = TextView(this).apply {
            text = "Опубликовать"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(25).toFloat()
            }
            setPadding(0, LayoutHelper.dp(16), 0, LayoutHelper.dp(16))
            setOnClickListener { 
                publishPost(input.text.toString())
            }
        }
        innerContent.addView(publishBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        root.addView(postCreationOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
    }

    private fun showPostCreationOverlay() {
        postCreationOverlay.visibility = View.VISIBLE
    }

    private fun hidePostCreationOverlay() {
        postCreationOverlay.visibility = View.GONE
    }

    private fun selectPostImage() {
        MediaPickerSheet.show(this, mode = MediaPickerSheet.Mode.POST) { uris, _ ->
            if (uris.isNotEmpty()) {
                val uri = uris.first()
                selectedImageFile = uriToFile(uri)
                overlayImageContainer.visibility = View.VISIBLE
                overlayImagePreview.load(uri)
            }
        }
    }

    fun uriToFile(uri: Uri): File? {
        val path = getPathFromUri(uri) ?: return null
        return File(path)
    }

    private fun getPathFromUri(uri: Uri): String? {
        val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA))
            }
        }
        return uri.path
    }

    private fun publishPost(text: String) {
        if (text.isBlank()) return
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = withContext(Dispatchers.IO) { 
                    apiRepo.createPost(token, text, selectedImageFile)
                }
                if (success) {
                    hidePostCreationOverlay()
                    loadPosts()
                    selectedImageFile = null
                    overlayImageContainer.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@NewsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
