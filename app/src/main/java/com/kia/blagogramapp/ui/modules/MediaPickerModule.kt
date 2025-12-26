package com.kia.blagogramapp.ui.modules

import android.content.ContentUris
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R

class MediaPickerSheet(context: Context) : LinearLayout(context) {

    private val recyclerView: RecyclerView
    private val adapter: MediaAdapter
    private val bottomBar: LinearLayout
    private val headerLayout: RelativeLayout
    private val albumTitle: TextView
    private val albumArrow: ImageView
    
    private var currentBucketId: String? = null
    private val albums = mutableListOf<Album>()

    init {
        orientation = VERTICAL
        val r = LayoutHelper.dp(20).toFloat()
        background = GradientDrawable().apply {
            setColor(DesignEngine.Theme.colorSheetBackground)
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }

        // Handle
        val handle = View(context).apply {
            background = GradientDrawable().apply {
                setColor(0x40808080.toInt())
                cornerRadius = LayoutHelper.dp(2).toFloat()
            }
        }
        addView(handle, LayoutHelper.createLinear(40, 4, 0f, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 8))

        // Header with Album Selection
        headerLayout = RelativeLayout(context).apply {
            setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), LayoutHelper.dp(8))
        }
        
        val titleContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { showAlbumPicker() }
        }
        
        albumTitle = TextView(context).apply {
            text = "Все медиа"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        titleContainer.addView(albumTitle)
        
        albumArrow = ImageView(context).apply {
            setImageResource(R.drawable.ic_chevron_right) // We'll rotate it for "down"
            rotation = 90f
            setColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN)
        }
        titleContainer.addView(albumArrow, LayoutHelper.createLinear(20, 20, 0f, 0, 4, 0, 0, 0))
        
        headerLayout.addView(titleContainer, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        
        val closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_close_small)
            setColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN)
            setOnClickListener { findDialog()?.dismiss() }
        }
        val closeParams = LayoutHelper.createRelative(24, 24)
        closeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        headerLayout.addView(closeBtn, closeParams)
        
        addView(headerLayout)

        // Grid
        recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 3)
            setPadding(LayoutHelper.dp(2), 0, LayoutHelper.dp(2), 0)
        }
        adapter = MediaAdapter { uris -> }
        recyclerView.adapter = adapter
        addView(recyclerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))

        // Bottom Bar
        bottomBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(12), 0, LayoutHelper.dp(16))
            setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
        }
        
        bottomBar.addView(createBottomItem(R.drawable.ic_photo, "Галерея", DesignEngine.Theme.colorBlue))
        bottomBar.addView(createBottomItem(R.drawable.ic_music, "Музыка", 0xFF2196F3.toInt()))
        bottomBar.addView(createBottomItem(R.drawable.ic_more, "Файл", 0xFF4CAF50.toInt()))
        bottomBar.addView(createBottomItem(R.drawable.ic_user, "Контакт", 0xFFFF9800.toInt()))
        
        addView(bottomBar)
        
        loadAlbums()
        loadPhotos(null)
    }

    private fun findDialog(): BottomSheetDialog? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is BottomSheetDialog) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun showAlbumPicker() {
        val popup = PopupWindow(context)
        val list = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            elevation = LayoutHelper.dp(8).toFloat()
        }
        
        list.adapter = object : RecyclerView.Adapter<AlbumViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
                val row = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
                    layoutParams = ViewGroup.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
                    
                    val outValue = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                    background = context.getDrawable(outValue.resourceId)
                }
                
                val img = ShapeableImageView(context).apply {
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(8).toFloat()).build()
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                row.addView(img, LayoutHelper.createLinear(48, 48))
                
                val textContainer = LinearLayout(context).apply {
                    orientation = VERTICAL
                    setPadding(LayoutHelper.dp(12), 0, 0, 0)
                }
                val name = TextView(context).apply {
                    textSize = 16f
                    setTextColor(DesignEngine.Theme.colorTextPrimary)
                    typeface = Typeface.DEFAULT_BOLD
                }
                val count = TextView(context).apply {
                    textSize = 14f
                    setTextColor(DesignEngine.Theme.colorTextSecondary)
                }
                textContainer.addView(name)
                textContainer.addView(count)
                row.addView(textContainer)
                
                return AlbumViewHolder(row, img, name, count)
            }

            override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
                val album = albums[position]
                holder.name.text = album.name
                holder.count.text = album.count.toString()
                holder.img.load(album.coverUri)
                holder.itemView.setOnClickListener {
                    albumTitle.text = album.name
                    loadPhotos(album.id)
                    popup.dismiss()
                }
            }

            override fun getItemCount() = albums.size
        }
        
        popup.contentView = list
        popup.width = LayoutHelper.dp(250)
        popup.height = LayoutHelper.dp(400)
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.showAsDropDown(albumTitle, 0, LayoutHelper.dp(8))
    }

    class AlbumViewHolder(view: View, val img: ImageView, val name: TextView, val count: TextView) : RecyclerView.ViewHolder(view)

    private fun createBottomItem(iconRes: Int, title: String, color: Int): LinearLayout {
        val item = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(12), 0)
        }
        
        val iconCircle = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = LayoutHelper.dp(24).toFloat()
            }
        }
        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        iconCircle.addView(icon, LayoutHelper.createFrame(24, 24, Gravity.CENTER))
        item.addView(iconCircle, LayoutHelper.createLinear(48, 48))
        
        val text = TextView(context).apply {
            this.text = title
            textSize = 11f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            setPadding(0, LayoutHelper.dp(4), 0, 0)
        }
        item.addView(text)
        
        return item
    }

    private fun loadAlbums() {
        albums.clear()
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media._ID
        )
        
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        
        val albumMap = mutableMapOf<String, Album>()
        var allCount = 0
        var firstUri: Uri? = null
        
        cursor?.use {
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val mediaIdCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            
            while (it.moveToNext()) {
                val name = it.getString(nameCol) ?: "Unknown"
                val id = it.getString(idCol)
                val mediaId = it.getLong(mediaIdCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                
                if (firstUri == null) firstUri = uri
                allCount++
                
                val album = albumMap[id]
                if (album == null) {
                    albumMap[id] = Album(id, name, uri, 1)
                } else {
                    album.count++
                }
            }
        }
        
        albums.add(Album(null, "Все медиа", firstUri, allCount))
        albums.addAll(albumMap.values.sortedByDescending { it.count })
    }

    private fun loadPhotos(bucketId: String?) {
        val photos = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = if (bucketId != null) "${MediaStore.Images.Media.BUCKET_ID} = ?" else null
        val selectionArgs = if (bucketId != null) arrayOf(bucketId) else null
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                photos.add(uri)
            }
        }
        adapter.items = photos
        adapter.notifyDataSetChanged()
    }

    data class Album(val id: String?, val name: String, val coverUri: Uri?, var count: Int)

    inner class MediaAdapter(var onSelectionChanged: (List<Uri>) -> Unit) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
        var items = listOf<Uri>()
        val selected = mutableListOf<Uri>()

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val container = view as FrameLayout
            val image: ShapeableImageView = container.getChildAt(0) as ShapeableImageView
            val checkText: TextView = container.getChildAt(1) as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val frame = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    LayoutHelper.dp(120)
                )
                setPadding(LayoutHelper.dp(1), LayoutHelper.dp(1), LayoutHelper.dp(1), LayoutHelper.dp(1))
            }

            val iv = ShapeableImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            frame.addView(iv)

            val tv = TextView(context).apply {
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LayoutHelper.createFrame(24, 24, Gravity.TOP or Gravity.END, 0, 6, 6, 0)
            }
            frame.addView(tv)

            return ViewHolder(frame)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = items[position]
            holder.image.load(uri)
            
            val selIndex = selected.indexOf(uri)
            val isSel = selIndex != -1
            
            holder.checkText.apply {
                if (isSel) {
                    text = (selIndex + 1).toString()
                    background = GradientDrawable().apply {
                        setColor(DesignEngine.Theme.colorBlue)
                        cornerRadius = LayoutHelper.dp(12).toFloat()
                        setStroke(LayoutHelper.dp(2), Color.WHITE)
                    }
                } else {
                    text = ""
                    background = GradientDrawable().apply {
                        setColor(0x33000000)
                        cornerRadius = LayoutHelper.dp(12).toFloat()
                        setStroke(LayoutHelper.dp(2), Color.WHITE)
                    }
                }
            }
            
            holder.container.setOnClickListener {
                if (selected.contains(uri)) {
                    selected.remove(uri)
                } else {
                    selected.add(uri)
                }
                notifyDataSetChanged()
                onSelectionChanged(selected.toList())
            }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        fun show(context: Context, onResult: (List<Uri>) -> Unit) {
            val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
            val sheet = MediaPickerSheet(context)
            
            val parent = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = ViewGroup.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorSheetBackground)
                    val r = LayoutHelper.dp(20).toFloat()
                    cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                }
            }
            parent.addView(sheet, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))
            
            val sendBtnContainer = FrameLayout(context).apply {
                setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(16))
                visibility = View.GONE
                setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
            }
            
            val sendBtn = TextView(context).apply {
                text = "ОТПРАВИТЬ"
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                textSize = 14f
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorBlue)
                    cornerRadius = LayoutHelper.dp(24).toFloat()
                }
                setPadding(LayoutHelper.dp(20), LayoutHelper.dp(12), LayoutHelper.dp(20), LayoutHelper.dp(12))
            }
            sendBtnContainer.addView(sendBtn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
            parent.addView(sendBtnContainer)
            
            sheet.adapter.onSelectionChanged = { uris ->
                sendBtnContainer.visibility = if (uris.isNotEmpty()) View.VISIBLE else View.GONE
                sendBtn.text = "ОТПРАВИТЬ (${uris.size})"
            }
            
            sendBtn.setOnClickListener {
                onResult(sheet.adapter.selected.toList())
                dialog.dismiss()
            }

            dialog.setContentView(parent)
            
            val behavior = BottomSheetBehavior.from(parent.parent as View)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = LayoutHelper.dp(500)
            
            dialog.show()
        }
    }
}
