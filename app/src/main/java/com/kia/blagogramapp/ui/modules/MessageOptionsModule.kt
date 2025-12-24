package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import coil.load

class MessageOptionsSheet(context: Context) : LinearLayout(context) {

    private val editRow: LinearLayout
    private val deleteRow: LinearLayout

    var onEditClick: (() -> Unit)? = null
    var onDeleteClick: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        val r = LayoutHelper.dp(20).toFloat()
        background = GradientDrawable().apply {
            setColor(DesignEngine.Theme.colorSheetBackground)
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
        setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(16))

        // Handle bar
        val handle = View(context).apply {
            background = GradientDrawable().apply {
                setColor(0x40808080.toInt())
                cornerRadius = LayoutHelper.dp(2).toFloat()
            }
        }
        addView(handle, LayoutHelper.createLinear(40, 4, 0f, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16))

        // Edit option
        editRow = createOptionRow(
            iconRes = R.drawable.ic_edit,
            text = "Редактировать",
            color = DesignEngine.Theme.colorTextPrimary
        )
        editRow.setOnClickListener { onEditClick?.invoke() }
        addView(editRow)

        // Delete option
        deleteRow = createOptionRow(
            iconRes = R.drawable.ic_delete,
            text = "Удалить",
            color = Color.RED
        )
        deleteRow.setOnClickListener { onDeleteClick?.invoke() }
        addView(deleteRow)
    }

    fun createOptionRow(iconRes: Int, text: String, color: Int): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(20), 0, LayoutHelper.dp(20), 0)
            
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            background = context.getDrawable(outValue.resourceId)
            isClickable = true
            isFocusable = true
        }

        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        row.addView(icon, LayoutHelper.createLinear(24, 24))

        val textView = TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 16f
            setPadding(LayoutHelper.dp(16), 0, 0, 0)
        }
        row.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        row.layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56)
        return row
    }

    companion object {
        fun show(context: Context, onEdit: () -> Unit, onDelete: () -> Unit) {
            val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
            val sheet = MessageOptionsSheet(context)
            sheet.onEditClick = {
                dialog.dismiss()
                onEdit()
            }
            sheet.onDeleteClick = {
                dialog.dismiss()
                onDelete()
            }
            dialog.setContentView(sheet)
            dialog.show()
        }

        fun showForChat(context: Context, pinned: Boolean, onPinToggle: () -> Unit, onDelete: () -> Unit) {
            val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
            val sheet = MessageOptionsSheet(context)
            sheet.removeAllViews()

            val handle = View(context).apply {
                background = GradientDrawable().apply {
                    setColor(0x40808080.toInt())
                    cornerRadius = LayoutHelper.dp(2).toFloat()
                }
            }
            sheet.addView(handle, LayoutHelper.createLinear(40, 4, 0f, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16))

            val pinRow = sheet.createOptionRow(
                iconRes = R.drawable.ic_pin,
                text = if (pinned) "Открепить" else "Закрепить",
                color = DesignEngine.Theme.colorTextPrimary
            )
            pinRow.setOnClickListener {
                dialog.dismiss()
                onPinToggle()
            }
            sheet.addView(pinRow)

            val deleteRow = sheet.createOptionRow(
                iconRes = R.drawable.ic_delete,
                text = "Удалить чат",
                color = Color.RED
            )
            deleteRow.setOnClickListener {
                dialog.dismiss()
                onDelete()
            }
            sheet.addView(deleteRow)

            dialog.setContentView(sheet)
            dialog.show()
        }

        fun showDeleteConfirm(context: Context, name: String, avatarUrl: String?, onConfirm: (Boolean) -> Unit) {
            val root = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(16))
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorCardBackground)
                    cornerRadius = LayoutHelper.dp(16).toFloat()
                }
            }

            // Header: Avatar + Title
            val header = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val avatar = ShapeableImageView(context).apply {
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(LayoutHelper.dp(24).toFloat())
                    .build()
                load("https://blagogram.ru/avatars/$avatarUrl") {
                    placeholder(R.drawable.ic_user_placeholder)
                    error(R.drawable.ic_user_placeholder)
                }
            }
            header.addView(avatar, LayoutHelper.createLinear(48, 48))

            val title = TextView(context).apply {
                text = "Удалить чат"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                setPadding(LayoutHelper.dp(16), 0, 0, 0)
            }
            header.addView(title)
            root.addView(header)

            // Message
            val message = TextView(context).apply {
                text = "Удалить чат с $name без возможности восстановления?"
                textSize = 16f
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                setPadding(0, LayoutHelper.dp(20), 0, 0)
            }
            root.addView(message)

            // Checkbox
            val checkbox = CheckBox(context).apply {
                text = "Также удалить для $name"
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                buttonTintList = ColorStateList.valueOf(DesignEngine.Theme.colorBlue)
                setPadding(LayoutHelper.dp(8), LayoutHelper.dp(16), 0, LayoutHelper.dp(16))
            }
            root.addView(checkbox)

            // Buttons
            val buttons = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.END
                setPadding(0, LayoutHelper.dp(8), 0, 0)
            }
            
            val btnCancel = TextView(context).apply {
                text = "Отмена"
                setTextColor(DesignEngine.Theme.colorBlue)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                background = context.getDrawable(outValue.resourceId)
                isClickable = true
            }
            
            val btnDelete = TextView(context).apply {
                text = "Удалить чат"
                setTextColor(Color.RED)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                background = context.getDrawable(outValue.resourceId)
                isClickable = true
            }

            buttons.addView(btnCancel)
            buttons.addView(btnDelete)
            root.addView(buttons)

            val dialog = AlertDialog.Builder(context).create()
            dialog.setView(root)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnCancel.setOnClickListener { dialog.dismiss() }
            btnDelete.setOnClickListener {
                dialog.dismiss()
                onConfirm(checkbox.isChecked)
            }

            dialog.show()
        }
    }
}
