package com.kmail.thekami.ui.home

enum class SortOrder { NEWEST, OLDEST, UNREAD_FIRST, READ_FIRST }

enum class ListFilter { ALL, UNREAD, STARRED, ATTACHMENTS }

data class ListOptions(
    val sort: SortOrder = SortOrder.NEWEST,
    val filter: ListFilter = ListFilter.ALL,
) {
    companion object {
        fun apply(items: List<MessageItem>, options: ListOptions): List<MessageItem> {
            val filtered = items.filter {
                when (options.filter) {
                    ListFilter.ALL -> true
                    ListFilter.UNREAD -> !it.isRead
                    ListFilter.STARRED -> it.isStarred
                    ListFilter.ATTACHMENTS -> it.hasAttachments
                }
            }
            return when (options.sort) {
                SortOrder.NEWEST -> filtered.sortedByDescending { it.receivedAtMillis }
                SortOrder.OLDEST -> filtered.sortedBy { it.receivedAtMillis }
                SortOrder.UNREAD_FIRST ->
                    filtered.sortedWith(compareBy<MessageItem> { it.isRead }.thenByDescending { it.receivedAtMillis })
                SortOrder.READ_FIRST ->
                    filtered.sortedWith(compareBy<MessageItem> { !it.isRead }.thenByDescending { it.receivedAtMillis })
            }
        }
    }
}
