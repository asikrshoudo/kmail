package com.kmail.thekami.domain.model

class DraftAttachment(val name: String, val mimeType: String, val bytes: ByteArray)

class Draft(
    val id: String,
    val accountId: String?,
    val to: String,
    val cc: String,
    val bcc: String,
    val subject: String,
    val body: String,
    val inReplyTo: String?,
    val references: List<String>,
    val attachments: List<DraftAttachment>,
    val updatedAtMillis: Long,
)

data class DraftSummary(
    val id: String,
    val to: String,
    val subject: String,
    val preview: String,
    val updatedAtMillis: Long,
)
