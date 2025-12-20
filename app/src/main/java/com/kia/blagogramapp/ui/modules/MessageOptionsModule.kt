package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R

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
            iconRes = R.drawable.ic_close_small,
            text = "Удалить",
            color = Color.RED
        )
        deleteRow.setOnClickListener { onDeleteClick?.invoke() }
        addView(deleteRow)
    }

    private fun createOptionRow(iconRes: Int, text: String, color: Int): LinearLayout {
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
            // Если BottomSheetDialogTheme все еще подсвечивается, попробуйте сделать Build -> Clean Project
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
    }
}
