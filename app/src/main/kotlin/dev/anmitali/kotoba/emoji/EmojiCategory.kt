package dev.anmitali.kotoba.emoji

data class EmojiCategory(
    val id: String,
    val name: String,
    val icon: String,
    val emoji: List<String>,
)
