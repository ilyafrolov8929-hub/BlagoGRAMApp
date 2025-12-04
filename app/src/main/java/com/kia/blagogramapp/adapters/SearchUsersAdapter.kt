package com.kia.blagogramapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kia.blagogramapp.R
import com.kia.blagogramapp.models.SearchUserItem

class SearchUsersAdapter(
    val items: List<SearchUserItem>,
    val onClick: (SearchUserItem) -> Unit
) : RecyclerView.Adapter<SearchUsersAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.itemAvatar)
        val name: TextView = v.findViewById(R.id.itemName)
        val username: TextView = v.findViewById(R.id.itemUsername)

        init {
            v.setOnClickListener { onClick(items[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user, parent, false)
        return Holder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: Holder, pos: Int) {
        val u = items[pos]

        h.name.text = if (u.display_name.isNullOrEmpty()) "Неизвестно" else u.display_name
        h.username.text = "@${u.username}"

        h.avatar.load("https://blagogram.ru/avatars/${u.avatar}") {
            placeholder(R.drawable.ic_user_placeholder)
        }
    }
}
