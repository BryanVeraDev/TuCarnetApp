package com.example.tucarnetapp.data.cache

object PhotoUrlMemoryCache {

    private const val EXPIRATION_MS = 600_000L // ✅ 600 segundos

    private var cachedPhotoKey: String? = null
    private var cachedUrl: String? = null
    private var expiresAt: Long = 0L

    fun get(photoKey: String): String? {
        val now = System.currentTimeMillis()

        return if (
            cachedPhotoKey == photoKey &&
            cachedUrl != null &&
            now < expiresAt
        ) {
            cachedUrl
        } else {
            null
        }
    }

    fun put(photoKey: String, url: String) {
        cachedPhotoKey = photoKey
        cachedUrl = url
        expiresAt = System.currentTimeMillis() + EXPIRATION_MS
    }

    fun clear() {
        cachedPhotoKey = null
        cachedUrl = null
        expiresAt = 0L
    }
}
