package dev.anmitali.kotoba.emoji

const val RecentEmojiLimit = 32

fun addRecent(current: List<String>, emoji: String, limit: Int = RecentEmojiLimit): List<String> =
    (listOf(emoji) + current.filter { it != emoji }).take(limit)
