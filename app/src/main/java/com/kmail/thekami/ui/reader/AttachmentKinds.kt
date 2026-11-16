package com.kmail.thekami.ui.reader

enum class AttachmentKind { IMAGE, PDF, DOCUMENT, ARCHIVE, OTHER }

object AttachmentKinds {
    private val IMAGES = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic")
    private val ARCHIVES = setOf("zip", "rar", "7z", "tar", "gz", "tgz")
    private val DOCS = setOf("doc", "docx", "odt", "rtf", "txt", "md", "xls", "xlsx", "ods", "csv", "ppt", "pptx", "odp")
    private val ARCHIVE_TYPES = setOf(
        "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
        "application/gzip", "application/x-tar",
    )

    fun of(fileName: String, mimeType: String): AttachmentKind {
        val type = mimeType.lowercase()
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            type.startsWith("image/") || ext in IMAGES -> AttachmentKind.IMAGE
            type == "application/pdf" || ext == "pdf" -> AttachmentKind.PDF
            type in ARCHIVE_TYPES || ext in ARCHIVES -> AttachmentKind.ARCHIVE
            type.startsWith("text/") || ext in DOCS || type.startsWith("application/msword") ||
                type.startsWith("application/vnd.openxmlformats") || type.startsWith("application/vnd.ms-") ->
                AttachmentKind.DOCUMENT
            else -> AttachmentKind.OTHER
        }
    }
}
