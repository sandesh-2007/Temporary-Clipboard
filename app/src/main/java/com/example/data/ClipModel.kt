package com.example.data

import java.util.UUID

data class ClipItem(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val content: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val type: String = "text", // "text", "file", "image"
    val burnOnRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 10 * 60 * 1000L
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    val remainingSeconds: Long
        get() = ((expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
}
