package com.antechrist.adherentsapp.ui.utils

private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L

fun canChangePhoto(lastUpdate: Long?, nowMs: Long = System.currentTimeMillis()): Boolean {
    if (lastUpdate == null) return true
    return (nowMs - lastUpdate) >= THIRTY_DAYS_MS
}

fun nextAllowedChangeAt(lastUpdate: Long): Long = lastUpdate + THIRTY_DAYS_MS
