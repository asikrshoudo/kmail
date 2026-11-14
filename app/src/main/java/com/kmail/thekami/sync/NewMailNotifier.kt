package com.kmail.thekami.sync

import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.domain.model.Account

interface NewMailNotifier {
    fun notifyNew(messages: List<MessageEntity>, accounts: List<Account>)
}
