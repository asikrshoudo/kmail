package com.kmail.thekami.mail

import com.kmail.thekami.domain.model.FolderRole
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderRolesTest {
    private fun role(name: String, sep: Char = '/', vararg attrs: String) =
        FolderRoles.detect(name, sep, attrs.toList())

    @Test
    fun inboxIgnoresCase() {
        assertEquals(FolderRole.INBOX, role("inbox"))
        assertEquals(FolderRole.INBOX, role("INBOX"))
    }

    @Test
    fun specialUseAttributesWinOverNames() {
        assertEquals(FolderRole.SENT, role("Weird name", '/', "\\HasNoChildren", "\\Sent"))
        assertEquals(FolderRole.TRASH, role("x", '/', "\\Trash"))
        assertEquals(FolderRole.SPAM, role("x", '/', "\\Junk"))
        assertEquals(FolderRole.DRAFTS, role("x", '/', "\\Drafts"))
        assertEquals(FolderRole.ARCHIVE, role("x", '/', "\\All"))
    }

    @Test
    fun namesWorkWithAnySeparatorPrefix() {
        assertEquals(FolderRole.SENT, role("INBOX.Sent", '.'))
        assertEquals(FolderRole.SENT, role("[Gmail]/Sent Mail"))
        assertEquals(FolderRole.TRASH, role("INBOX.Deleted Items", '.'))
        assertEquals(FolderRole.SPAM, role("INBOX.Junk", '.'))
    }

    @Test
    fun otherFoldersAreCustom() {
        assertEquals(FolderRole.CUSTOM, role("Projects/Alpha"))
        assertEquals(FolderRole.CUSTOM, role("Work", '.', "\\HasChildren"))
    }
}
