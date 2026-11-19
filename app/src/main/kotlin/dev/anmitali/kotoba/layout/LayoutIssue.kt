package dev.anmitali.kotoba.layout

enum class Severity { Error, Warning }

data class LayoutIssue(
    val path: String,
    val message: String,
    val severity: Severity,
)
