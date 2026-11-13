package com.kmail.thekami.mail

import com.kmail.thekami.domain.model.FolderRole
import java.util.Locale

internal object FolderRoles {
    /** Prefers the server's special-use attributes, then falls back to common folder names. */
    fun detect(fullName: String, separator: Char, attributes: List<String>): FolderRole {
        if (fullName.equals("INBOX", ignoreCase = true)) return FolderRole.INBOX
        val attrs = attributes.map { it.lowercase(Locale.ROOT) }
        when {
            "\\sent" in attrs -> return FolderRole.SENT
            "\\drafts" in attrs -> return FolderRole.DRAFTS
            "\\trash" in attrs -> return FolderRole.TRASH
            "\\junk" in attrs -> return FolderRole.SPAM
            "\\archive" in attrs || "\\all" in attrs -> return FolderRole.ARCHIVE
        }
        val leaf = fullName.substringAfterLast(separator).trim().lowercase(Locale.ROOT)
        return when (leaf) {
            "sent", "sent items", "sent mail", "sent messages" -> FolderRole.SENT
            "drafts", "draft" -> FolderRole.DRAFTS
            "trash", "deleted", "deleted items", "deleted messages", "bin" -> FolderRole.TRASH
            "spam", "junk", "junk e-mail", "junk email", "bulk mail" -> FolderRole.SPAM
            "archive", "archives", "all mail" -> FolderRole.ARCHIVE
            else -> FolderRole.CUSTOM
        }
    }
}
