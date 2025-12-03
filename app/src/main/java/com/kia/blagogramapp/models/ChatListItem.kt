package com.kia.blagogramapp.models

import com.kia.blagogramapp.model.Message

sealed class ChatListItem {
    data class Header(val title: String) : ChatListItem()
    data class Msg(val message: Message) : ChatListItem()
}
