package com.kia.blagogramapp.models

import com.kia.blagogramapp.model.Message

sealed class ChatListItem {
    data class Header(val title: String) : ChatListItem()
    object Space : ChatListItem()
    data class Msg(
        val message: Message,
        val isPrevSame: Boolean = false,
        val isNextSame: Boolean = false
    ) : ChatListItem()
    object UnreadSeparator : ChatListItem()
}
