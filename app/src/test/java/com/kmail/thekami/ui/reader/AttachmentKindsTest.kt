package com.kmail.thekami.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentKindsTest {
    @Test
    fun picksTheKindFromMimeTypeOrExtension() {
        assertEquals(AttachmentKind.IMAGE, AttachmentKinds.of("x.bin", "image/png"))
        assertEquals(AttachmentKind.IMAGE, AttachmentKinds.of("photo.JPG", "application/octet-stream"))
        assertEquals(AttachmentKind.PDF, AttachmentKinds.of("a.pdf", "application/octet-stream"))
        assertEquals(AttachmentKind.ARCHIVE, AttachmentKinds.of("a.zip", "application/zip"))
        assertEquals(AttachmentKind.DOCUMENT, AttachmentKinds.of("a.docx", "application/octet-stream"))
        assertEquals(AttachmentKind.DOCUMENT, AttachmentKinds.of("notes", "text/plain"))
        assertEquals(AttachmentKind.OTHER, AttachmentKinds.of("data", "application/octet-stream"))
        assertEquals(AttachmentKind.OTHER, AttachmentKinds.of("", ""))
    }
}
