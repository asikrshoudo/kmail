package com.kmail.thekami.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Every icon in the app, in one place. To change an icon, change its path data here.
 *
 * Icons come from Material Design Icons by Pictogrammers (https://pictogrammers.com/library/mdi/),
 * licensed under Apache 2.0. Keep that notice in the app's open source licenses.
 *
 * Icons marked mirror flip in right-to-left languages (arrows, chevrons, send, reply).
 */
object AppIcons {
    /** arrow-left */
    val Back: ImageVector by lazy {
        icon(
            "Back",
            "M20 11v2H8l5.5 5.5-1.42 1.42L4.16 12l7.92-7.92L13.5 5.5 8 11z", mirror = true,
        )
    }
    /** menu */
    val Menu: ImageVector by lazy {
        icon(
            "Menu",
            "M3 6h18v2H3zm0 5h18v2H3zm0 5h18v2H3z",
        )
    }
    /** dots-vertical */
    val More: ImageVector by lazy {
        icon(
            "More",
            "M12 16a2 2 0 0 1 2 2 2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1 2-2m0-6a2 2 0 0 1 2 2 2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1 2-2m0-6a2 2 0 0 1 2 2 2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1 2-2",
        )
    }
    /** close */
    val Close: ImageVector by lazy {
        icon(
            "Close",
            "M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
        )
    }
    /** check */
    val Check: ImageVector by lazy {
        icon(
            "Check",
            "M21 7 9 19l-5.5-5.5 1.41-1.41L9 16.17 19.59 5.59z",
        )
    }
    /** magnify */
    val Search: ImageVector by lazy {
        icon(
            "Search",
            "M9.5 3A6.5 6.5 0 0 1 16 9.5c0 1.61-.59 3.09-1.56 4.23l.27.27h.79l5 5-1.5 1.5-5-5v-.79l-.27-.27A6.52 6.52 0 0 1 9.5 16 6.5 6.5 0 0 1 3 9.5 6.5 6.5 0 0 1 9.5 3m0 2C7 5 5 7 5 9.5S7 14 9.5 14 14 12 14 9.5 12 5 9.5 5",
        )
    }
    /** chevron-right */
    val ChevronRight: ImageVector by lazy {
        icon(
            "ChevronRight",
            "M8.59 16.58 13.17 12 8.59 7.41 10 6l6 6-6 6z", mirror = true,
        )
    }
    /** chevron-down */
    val ChevronDown: ImageVector by lazy {
        icon(
            "ChevronDown",
            "M7.41 8.58 12 13.17l4.59-4.59L18 10l-6 6-6-6z",
        )
    }
    /** chevron-up */
    val ChevronUp: ImageVector by lazy {
        icon(
            "ChevronUp",
            "M7.41 15.41 12 10.83l4.59 4.58L18 14l-6-6-6 6z",
        )
    }
    /** open-in-new */
    val OpenInNew: ImageVector by lazy {
        icon(
            "OpenInNew",
            "M14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3m-2 16H5V5h7V3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7h-2z", mirror = true,
        )
    }
    /** trash-can-outline */
    val Trash: ImageVector by lazy {
        icon(
            "Trash",
            "M9 3v1H4v2h1v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6h1V4h-5V3zM7 6h10v13H7zm2 2v9h2V8zm4 0v9h2V8z",
        )
    }
    /** archive-outline */
    val Archive: ImageVector by lazy {
        icon(
            "Archive",
            "M20 21H4V10h2v9h12v-9h2zM3 3h18v6H3zm6.5 8h5c.28 0 .5.22.5.5V13H9v-1.5c0-.28.22-.5.5-.5M5 5v2h14V5z",
        )
    }
    /** star-outline */
    val StarOutline: ImageVector by lazy {
        icon(
            "StarOutline",
            "m12 15.39-3.76 2.27.99-4.28-3.32-2.88 4.38-.37L12 6.09l1.71 4.04 4.38.37-3.32 2.88.99 4.28M22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.45 4.73L5.82 21 12 17.27 18.18 21l-1.64-7.03z",
        )
    }
    /** star */
    val Star: ImageVector by lazy {
        icon(
            "Star",
            "M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.62L12 2 9.19 8.62 2 9.24l5.45 4.73L5.82 21z",
        )
    }
    /** reply-outline */
    val Reply: ImageVector by lazy {
        icon(
            "Reply",
            "M8 9.8v.9l1.7.3c2.6.4 4.5 1.4 5.9 2.7-1.7-.5-3.5-.8-5.6-.8H8v1.3L5.8 12zM10 5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11", mirror = true,
        )
    }
    /** reply-all-outline */
    val ReplyAll: ImageVector by lazy {
        icon(
            "ReplyAll",
            "M11 9.8v.9l1.7.2c2.6.4 4.5 1.4 5.9 2.7-1.7-.5-3.5-.8-5.6-.8h-2v1.3L8.8 12zM13 5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11M7 8V5l-7 7 7 7v-3l-4-4", mirror = true,
        )
    }
    /** reply-outline, turned around */
    val Forward: ImageVector by lazy {
        icon(
            "Forward",
            "M8 9.8v.9l1.7.3c2.6.4 4.5 1.4 5.9 2.7-1.7-.5-3.5-.8-5.6-.8H8v1.3L5.8 12zM10 5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11", mirror = true, flip = true,
        )
    }
    /** send-outline */
    val Send: ImageVector by lazy {
        icon(
            "Send",
            "m4 6.03 7.5 3.22-7.5-1zm7.5 8.72L4 17.97v-2.22zM2 3v7l15 2-15 2v7l21-9z", mirror = true,
        )
    }
    /** email-mark-as-unread */
    val MarkUnread: ImageVector by lazy {
        icon(
            "MarkUnread",
            "M18.84 7h-2.62L10.5 4 4 7.41V17a2 2 0 0 1-2-2V7.17c0-.67.28-1.11.81-1.36L10.5 2l7.55 3.81c.45.28.73.69.79 1.19M7 8h13a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2m13 3.67V10l-6.5 3.31L7 10v1.67L13.5 15z",
        )
    }
    /** pencil-outline */
    val Pencil: ImageVector by lazy {
        icon(
            "Pencil",
            "m14.06 9 .94.94L5.92 19H5v-.92zm3.6-6c-.25 0-.51.1-.7.29l-1.83 1.83 3.75 3.75 1.83-1.83c.39-.39.39-1.04 0-1.41l-2.34-2.34c-.2-.2-.45-.29-.71-.29m-3.6 3.19L3 17.25V21h3.75L17.81 9.94z",
        )
    }
    /** paperclip */
    val Attachment: ImageVector by lazy {
        icon(
            "Attachment",
            "M16.5 6v11.5a4 4 0 0 1-4 4 4 4 0 0 1-4-4V5A2.5 2.5 0 0 1 11 2.5 2.5 2.5 0 0 1 13.5 5v10.5a1 1 0 0 1-1 1 1 1 0 0 1-1-1V6H10v9.5a2.5 2.5 0 0 0 2.5 2.5 2.5 2.5 0 0 0 2.5-2.5V5a4 4 0 0 0-4-4 4 4 0 0 0-4 4v12.5a5.5 5.5 0 0 0 5.5 5.5 5.5 5.5 0 0 0 5.5-5.5V6z",
        )
    }
    /** download-outline */
    val Download: ImageVector by lazy {
        icon(
            "Download",
            "M13 5v6h1.17L12 13.17 9.83 11H11V5zm2-2H9v6H5l7 7 7-7h-4zm4 15H5v2h14z",
        )
    }
    /** inbox-outline */
    val Inbox: ImageVector by lazy {
        icon(
            "Inbox",
            "M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2M5 19v-2h3.13a4.13 4.13 0 0 0 1.27 2m9.6 0h-4.4a4.13 4.13 0 0 0 1.27-2H19m0-2h-5v1a2 2 0 0 1-4 0v-1H5V5h14Z",
        )
    }
    /** inbox-multiple-outline */
    val InboxAll: ImageVector by lazy {
        icon(
            "InboxAll",
            "M19 3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM5 10v2h4.4c-.6-.53-1.06-1.22-1.27-2zm14 2v-2h-3.13c-.21.78-.67 1.47-1.27 2zm0-4V5H5v3h5v1c0 1.07.93 2 2 2s2-.93 2-2V8zm2 11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4h7v1c0 1.07.93 2 2 2s2-.93 2-2v-1h7zM5 17v2h4.4c-.6-.53-1.06-1.22-1.27-2zm14 2v-2h-3.13c-.21.78-.67 1.47-1.27 2z",
        )
    }
    /** email-outline */
    val Mail: ImageVector by lazy {
        icon(
            "Mail",
            "M22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2zm-2 0-8 5-8-5zm0 12H4V8l8 5 8-5z",
        )
    }
    /** email-open-outline */
    val MailOpen: ImageVector by lazy {
        icon(
            "MailOpen",
            "M21.03 6.29 12 .64 2.97 6.29C2.39 6.64 2 7.27 2 8v10c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-.73-.39-1.36-.97-1.71M20 18H4v-8l8 5 8-5zm-8-5L4 8l8-5 8 5z",
        )
    }
    /** note-edit-outline */
    val NoteEdit: ImageVector by lazy {
        icon(
            "NoteEdit",
            "m18.13 12 1.26-1.26c.44-.44 1-.68 1.61-.74V9l-6-6H5c-1.11 0-2 .89-2 2v14a2 2 0 0 0 2 2h6v-1.87l.13-.13H5V5h7v7zM14 4.5l5.5 5.5H14zm5.13 9.33 2.04 2.04L15.04 22H13v-2.04zm3.72.36-.98.98-2.04-2.04.98-.98c.19-.2.52-.2.72 0l1.32 1.32c.2.2.2.53 0 .72",
        )
    }
    /** folder-outline */
    val Folder: ImageVector by lazy {
        icon(
            "Folder",
            "M20 18H4V8h16m0-2h-8l-2-2H4c-1.11 0-2 .89-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2",
        )
    }
    /** folder-open-outline */
    val FolderOpen: ImageVector by lazy {
        icon(
            "FolderOpen",
            "M6.1 10 4 18V8h17a2 2 0 0 0-2-2h-7l-2-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h15c.9 0 1.7-.6 1.9-1.5l2.3-8.5zM19 18H6l1.6-6h13z",
        )
    }
    /** folder-zip-outline */
    val FolderZip: ImageVector by lazy {
        icon(
            "FolderZip",
            "M20 6h-8l-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2m0 12h-4v-2h-2v2H4V8h10v2h2V8h4zm-4-6v-2h2v2zm-2 0h2v2h-2zm4 4h-2v-2h2z",
        )
    }
    /** alert-octagon-outline */
    val Spam: ImageVector by lazy {
        icon(
            "Spam",
            "M8.27 3 3 8.27v7.46L8.27 21h7.46C17.5 19.24 21 15.73 21 15.73V8.27L15.73 3M9.1 5h5.8L19 9.1v5.8L14.9 19H9.1L5 14.9V9.1m6 5.9h2v2h-2zm0-8h2v6h-2z",
        )
    }
    /** account-plus-outline */
    val AddAccount: ImageVector by lazy {
        icon(
            "AddAccount",
            "M15 4a4 4 0 0 0-4 4 4 4 0 0 0 4 4 4 4 0 0 0 4-4 4 4 0 0 0-4-4m0 1.9a2.1 2.1 0 1 1 0 4.2A2.1 2.1 0 0 1 12.9 8 2.1 2.1 0 0 1 15 5.9M4 7v3H1v2h3v3h2v-3h3v-2H6V7zm11 6c-2.67 0-8 1.33-8 4v3h16v-3c0-2.67-5.33-4-8-4m0 1.9c2.97 0 6.1 1.46 6.1 2.1v1.1H8.9V17c0-.64 3.1-2.1 6.1-2.1",
        )
    }
    /** image-outline */
    val Image: ImageVector by lazy {
        icon(
            "Image",
            "M19 19H5V5h14m0-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2m-5.04 9.29-2.75 3.54-1.96-2.36L6.5 17h11z",
        )
    }
    /** file-pdf-box */
    val FilePdf: ImageVector by lazy {
        icon(
            "FilePdf",
            "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2m-9.5 8.5c0 .8-.7 1.5-1.5 1.5H7v2H5.5V9H8c.8 0 1.5.7 1.5 1.5zm5 2c0 .8-.7 1.5-1.5 1.5h-2.5V9H13c.8 0 1.5.7 1.5 1.5zm4-3H17v1h1.5V13H17v2h-1.5V9h3zm-6.5 0h1v3h-1zm-5 0h1v1H7z",
        )
    }
    /** file-document-outline */
    val Document: ImageVector by lazy {
        icon(
            "Document",
            "M6 2a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm0 2h7v5h5v11H6zm2 8v2h8v-2zm0 4v2h5v-2z",
        )
    }
    /** file-outline */
    val File: ImageVector by lazy {
        icon(
            "File",
            "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zm4 18H6V4h7v5h5z",
        )
    }
    /** cog-outline */
    val Settings: ImageVector by lazy {
        icon(
            "Settings",
            "M12 8a4 4 0 0 1 4 4 4 4 0 0 1-4 4 4 4 0 0 1-4-4 4 4 0 0 1 4-4m0 2a2 2 0 0 0-2 2 2 2 0 0 0 2 2 2 2 0 0 0 2-2 2 2 0 0 0-2-2m-2 12c-.25 0-.46-.18-.5-.42l-.37-2.65c-.63-.25-1.17-.59-1.69-.99l-2.49 1.01c-.22.08-.49 0-.61-.22l-2-3.46a.493.493 0 0 1 .12-.64l2.11-1.66L4.5 12l.07-1-2.11-1.63a.493.493 0 0 1-.12-.64l2-3.46c.12-.22.39-.31.61-.22l2.49 1c.52-.39 1.06-.73 1.69-.98l.37-2.65c.04-.24.25-.42.5-.42h4c.25 0 .46.18.5.42l.37 2.65c.63.25 1.17.59 1.69.98l2.49-1c.22-.09.49 0 .61.22l2 3.46c.13.22.07.49-.12.64L19.43 11l.07 1-.07 1 2.11 1.63c.19.15.25.42.12.64l-2 3.46c-.12.22-.39.31-.61.22l-2.49-1c-.52.39-1.06.73-1.69.98l-.37 2.65c-.04.24-.25.42-.5.42zm1.25-18-.37 2.61c-1.2.25-2.26.89-3.03 1.78L5.44 7.35l-.75 1.3L6.8 10.2a5.55 5.55 0 0 0 0 3.6l-2.12 1.56.75 1.3 2.43-1.04c.77.88 1.82 1.52 3.01 1.76l.37 2.62h1.52l.37-2.61c1.19-.25 2.24-.89 3.01-1.77l2.43 1.04.75-1.3-2.12-1.55c.4-1.17.4-2.44 0-3.61l2.11-1.55-.75-1.3-2.41 1.04a5.42 5.42 0 0 0-3.03-1.77L12.75 4z",
        )
    }
    /** account-circle-outline */
    val AccountCircle: ImageVector by lazy {
        icon(
            "AccountCircle",
            "M12 2A10 10 0 0 0 2 12a10 10 0 0 0 10 10 10 10 0 0 0 10-10A10 10 0 0 0 12 2M7.07 18.28c.43-.9 3.05-1.78 4.93-1.78s4.5.88 4.93 1.78A7.9 7.9 0 0 1 12 20c-1.86 0-3.57-.64-4.93-1.72m11.29-1.45c-1.43-1.74-4.9-2.33-6.36-2.33s-4.93.59-6.36 2.33A7.93 7.93 0 0 1 4 12c0-4.41 3.59-8 8-8s8 3.59 8 8c0 1.82-.62 3.5-1.64 4.83M12 6c-1.94 0-3.5 1.56-3.5 3.5S10.06 13 12 13s3.5-1.56 3.5-3.5S13.94 6 12 6m0 5a1.5 1.5 0 0 1-1.5-1.5A1.5 1.5 0 0 1 12 8a1.5 1.5 0 0 1 1.5 1.5A1.5 1.5 0 0 1 12 11",
        )
    }
    /** palette-outline */
    val Palette: ImageVector by lazy {
        icon(
            "Palette",
            "M12 22A10 10 0 0 1 2 12 10 10 0 0 1 12 2c5.5 0 10 4 10 9a6 6 0 0 1-6 6h-1.8c-.3 0-.5.2-.5.5 0 .1.1.2.1.3.4.5.6 1.1.6 1.7.1 1.4-1 2.5-2.4 2.5m0-18a8 8 0 0 0-8 8 8 8 0 0 0 8 8c.3 0 .5-.2.5-.5 0-.2-.1-.3-.1-.4-.4-.5-.6-1-.6-1.6 0-1.4 1.1-2.5 2.5-2.5H16a4 4 0 0 0 4-4c0-3.9-3.6-7-8-7m-5.5 6c.8 0 1.5.7 1.5 1.5S7.3 13 6.5 13 5 12.3 5 11.5 5.7 10 6.5 10m3-4c.8 0 1.5.7 1.5 1.5S10.3 9 9.5 9 8 8.3 8 7.5 8.7 6 9.5 6m5 0c.8 0 1.5.7 1.5 1.5S15.3 9 14.5 9 13 8.3 13 7.5 13.7 6 14.5 6m3 4c.8 0 1.5.7 1.5 1.5s-.7 1.5-1.5 1.5-1.5-.7-1.5-1.5.7-1.5 1.5-1.5",
        )
    }
    /** translate */
    val Language: ImageVector by lazy {
        icon(
            "Language",
            "m12.87 15.07-2.54-2.51.03-.03A17.5 17.5 0 0 0 14.07 6H17V4h-7V2H8v2H1v2h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2zm-2.62 7 1.62-4.33L19.12 17z",
        )
    }
    /** sync */
    val Sync: ImageVector by lazy {
        icon(
            "Sync",
            "M12 18a6 6 0 0 1-6-6c0-1 .25-1.97.7-2.8L5.24 7.74A7.93 7.93 0 0 0 4 12a8 8 0 0 0 8 8v3l4-4-4-4m0-11V1L8 5l4 4V6a6 6 0 0 1 6 6c0 1-.25 1.97-.7 2.8l1.46 1.46A7.93 7.93 0 0 0 20 12a8 8 0 0 0-8-8",
        )
    }
    /** information-outline */
    val Info: ImageVector by lazy {
        icon(
            "Info",
            "M11 9h2V7h-2m1 13c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8m0-18A10 10 0 0 0 2 12a10 10 0 0 0 10 10 10 10 0 0 0 10-10A10 10 0 0 0 12 2m-1 15h2v-6h-2z",
        )
    }
    /** qrcode */
    val QrCode: ImageVector by lazy {
        icon(
            "QrCode",
            "M3 11h2v2H3zm8-6h2v4h-2zm-2 6h4v4h-2v-2H9zm6 0h2v2h2v-2h2v2h-2v2h2v4h-2v2h-2v-2h-4v2h-2v-4h4v-2h2v-2h-2zm4 8v-4h-2v4zM15 3h6v6h-6zm2 2v2h2V5zM3 3h6v6H3zm2 2v2h2V5zM3 15h6v6H3zm2 2v2h2v-2z",
        )
    }
    /** qrcode-scan */
    val QrScan: ImageVector by lazy {
        icon(
            "QrScan",
            "M4 4h6v6H4zm16 0v6h-6V4zm-6 11h2v-2h-2v-2h2v2h2v-2h2v2h-2v2h2v3h-2v2h-2v-2h-3v2h-2v-4h3zm2 0v3h2v-3zM4 20v-6h6v6zM6 6v2h2V6zm10 0v2h2V6zM6 16v2h2v-2zm-2-5h2v2H4zm5 0h4v4h-2v-2H9zm2-5h2v4h-2zM2 2v4H0V2a2 2 0 0 1 2-2h4v2zm20-2a2 2 0 0 1 2 2v4h-2V2h-4V0zM2 18v4h4v2H2a2 2 0 0 1-2-2v-4zm20 4v-4h2v4a2 2 0 0 1-2 2h-4v-2z",
        )
    }
    /** lock-outline */
    val Lock: ImageVector by lazy {
        icon(
            "Lock",
            "M12 17a2 2 0 0 1-2-2c0-1.11.89-2 2-2a2 2 0 0 1 2 2 2 2 0 0 1-2 2m6 3V10H6v10zm0-12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V10c0-1.11.89-2 2-2h1V6a5 5 0 0 1 5-5 5 5 0 0 1 5 5v2zm-6-5a3 3 0 0 0-3 3v2h6V6a3 3 0 0 0-3-3",
        )
    }
    /** eye-outline */
    val Eye: ImageVector by lazy {
        icon(
            "Eye",
            "M12 9a3 3 0 0 1 3 3 3 3 0 0 1-3 3 3 3 0 0 1-3-3 3 3 0 0 1 3-3m0-4.5c5 0 9.27 3.11 11 7.5-1.73 4.39-6 7.5-11 7.5S2.73 16.39 1 12c1.73-4.39 6-7.5 11-7.5M3.18 12a9.821 9.821 0 0 0 17.64 0 9.821 9.821 0 0 0-17.64 0",
        )
    }
    /** eye-off-outline */
    val EyeOff: ImageVector by lazy {
        icon(
            "EyeOff",
            "M2 5.27 3.28 4 20 20.72 18.73 22l-3.08-3.08c-1.15.38-2.37.58-3.65.58-5 0-9.27-3.11-11-7.5.69-1.76 1.79-3.31 3.19-4.54zM12 9a3 3 0 0 1 3 3 3 3 0 0 1-.17 1L11 9.17A3 3 0 0 1 12 9m0-4.5c5 0 9.27 3.11 11 7.5a11.8 11.8 0 0 1-4 5.19l-1.42-1.43A9.86 9.86 0 0 0 20.82 12 9.82 9.82 0 0 0 12 6.5c-1.09 0-2.16.18-3.16.5L7.3 5.47c1.44-.62 3.03-.97 4.7-.97M3.18 12A9.82 9.82 0 0 0 12 17.5c.69 0 1.37-.07 2-.21L11.72 15A3.064 3.064 0 0 1 9 12.28L5.6 8.87c-.99.85-1.82 1.91-2.42 3.13",
        )
    }
    /** at */
    val At: ImageVector by lazy {
        icon(
            "At",
            "M12 15c.81 0 1.5-.3 2.11-.89.59-.61.89-1.3.89-2.11s-.3-1.5-.89-2.11C13.5 9.3 12.81 9 12 9s-1.5.3-2.11.89C9.3 10.5 9 11.19 9 12s.3 1.5.89 2.11c.61.59 1.3.89 2.11.89m0-13c2.75 0 5.1 1 7.05 2.95S22 9.25 22 12v1.45c0 1-.35 1.85-1 2.55-.7.67-1.5 1-2.5 1-1.2 0-2.19-.5-2.94-1.5-1 1-2.18 1.5-3.56 1.5-1.37 0-2.55-.5-3.54-1.46C7.5 14.55 7 13.38 7 12c0-1.37.5-2.55 1.46-3.54C9.45 7.5 10.63 7 12 7c1.38 0 2.55.5 3.54 1.46C16.5 9.45 17 10.63 17 12v1.45c0 .41.16.77.46 1.08s.65.47 1.04.47c.42 0 .77-.16 1.07-.47s.43-.67.43-1.08V12c0-2.19-.77-4.07-2.35-5.65S14.19 4 12 4s-4.07.77-5.65 2.35S4 9.81 4 12s.77 4.07 2.35 5.65S9.81 20 12 20h5v2h-5c-2.75 0-5.1-1-7.05-2.95S2 14.75 2 12s1-5.1 2.95-7.05S9.25 2 12 2",
        )
    }
    /** fingerprint */
    val Fingerprint: ImageVector by lazy {
        icon(
            "Fingerprint",
            "M17.81 4.47c-.08 0-.16-.02-.23-.06C15.66 3.42 14 3 12 3c-1.97 0-3.85.47-5.56 1.41-.24.13-.54.04-.68-.2a.506.506 0 0 1 .2-.68C7.82 2.5 9.86 2 12 2s4 .47 6.04 1.5c.25.15.34.45.21.69a.48.48 0 0 1-.44.28M3.5 9.72c-.1 0-.2-.03-.29-.09a.517.517 0 0 1-.12-.7c.99-1.4 2.25-2.5 3.75-3.27C10 4.04 14 4.03 17.15 5.65c1.5.77 2.76 1.85 3.75 3.25.16.22.1.54-.12.7-.23.16-.54.11-.7-.1a9.26 9.26 0 0 0-3.39-2.96c-2.87-1.47-6.54-1.47-9.4.01-1.36.7-2.5 1.7-3.4 2.95-.08.15-.23.22-.39.22m6.25 12.07c-.13 0-.25-.05-.35-.15-.87-.87-1.34-1.43-2.01-2.64-.69-1.23-1.05-2.73-1.05-4.34 0-2.97 2.54-5.39 5.66-5.39s5.66 2.42 5.66 5.39a.5.5 0 0 1-.5.5.5.5 0 0 1-.5-.5c0-2.42-2.09-4.39-4.66-4.39s-4.66 1.97-4.66 4.39c0 1.44.32 2.77.93 3.84.64 1.16 1.08 1.65 1.85 2.43.19.2.19.51 0 .71-.12.1-.24.15-.37.15m7.17-1.85c-1.19 0-2.24-.3-3.1-.89-1.49-1.01-2.38-2.65-2.38-4.39a.5.5 0 0 1 .5-.5.5.5 0 0 1 .5.5c0 1.41.72 2.74 1.94 3.56.71.48 1.54.71 2.54.71.24 0 .64-.03 1.04-.1.27-.05.54.13.58.41.05.26-.13.53-.41.58-.57.11-1.07.12-1.21.12M14.91 22h-.13c-1.59-.46-2.63-1.05-3.72-2.12a7.28 7.28 0 0 1-2.17-5.22c0-1.62 1.38-2.94 3.08-2.94s3.08 1.32 3.08 2.94c0 1.07.95 1.94 2.08 1.94 1.15 0 2.08-.87 2.08-1.94 0-3.77-3.25-6.83-7.25-6.83-2.84 0-5.46 1.58-6.61 4.03-.39.81-.59 1.76-.59 2.8 0 .78.07 2.01.67 3.61.1.26-.03.55-.29.64a.504.504 0 0 1-.64-.29c-.5-1.31-.73-2.62-.73-3.96 0-1.2.23-2.29.68-3.24 1.33-2.79 4.28-4.6 7.51-4.6 4.54 0 8.25 3.51 8.25 7.83 0 1.62-1.38 2.94-3.08 2.94s-3.08-1.32-3.08-2.94c0-1.07-.93-1.94-2.08-1.94s-2.08.87-2.08 1.94c0 1.71.66 3.31 1.87 4.51.95.94 1.86 1.46 3.27 1.84.27.08.42.36.35.62-.05.23-.26.38-.47.38",
        )
    }
    /** server-outline */
    val Server: ImageVector by lazy {
        icon(
            "Server",
            "M2 4.6v4.8c0 .9.5 1.6 1.2 1.6h17.7c.6 0 1.2-.7 1.2-1.6V4.6C22 3.7 21.5 3 20.8 3H3.2C2.5 3 2 3.7 2 4.6M10 8V6H9v2zM5 8h2V6H5zm15 1H4V5h16zM2 14.6v4.8c0 .9.5 1.6 1.2 1.6h17.7c.6 0 1.2-.7 1.2-1.6v-4.8c0-.9-.5-1.6-1.2-1.6H3.2c-.7 0-1.2.7-1.2 1.6m8 3.4v-2H9v2zm-5 0h2v-2H5zm15 1H4v-4h16z",
        )
    }
    /** account-outline */
    val Account: ImageVector by lazy {
        icon(
            "Account",
            "M12 4a4 4 0 0 1 4 4 4 4 0 0 1-4 4 4 4 0 0 1-4-4 4 4 0 0 1 4-4m0 2a2 2 0 0 0-2 2 2 2 0 0 0 2 2 2 2 0 0 0 2-2 2 2 0 0 0-2-2m0 7c2.67 0 8 1.33 8 4v3H4v-3c0-2.67 5.33-4 8-4m0 1.9c-2.97 0-6.1 1.46-6.1 2.1v1.1h12.2V17c0-.64-3.13-2.1-6.1-2.1",
        )
    }
}

private fun icon(
    name: String,
    pathData: String,
    mirror: Boolean = false,
    flip: Boolean = false,
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = mirror,
    )
    if (flip) builder.addGroup(scaleX = -1f, pivotX = 12f)
    builder.addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black),
    )
    if (flip) builder.clearGroup()
    return builder.build()
}
