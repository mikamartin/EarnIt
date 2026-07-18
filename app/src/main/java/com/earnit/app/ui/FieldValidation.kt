package com.earnit.app.ui

// Pure field-input transforms shared by every Compose text field that caps length or
// filters to digits, extracted from onValueChange blocks so they're testable without Compose.

fun acceptWithinLimit(
    current: String,
    incoming: String,
    max: Int,
): String = if (incoming.length <= max) incoming else current

fun String.digitsOnly(): String = filter { it.isDigit() }
