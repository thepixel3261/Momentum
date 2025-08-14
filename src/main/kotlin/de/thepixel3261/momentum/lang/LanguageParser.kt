/*
 * [Momentum] Rewards and More.
 * Copyright (C) 2025 thepixel3261
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Additional terms (Section 7, AGPL‑3.0-or‑later):
 * If you modify and run this plugin on a publicly accessible Minecraft server,
 * you must publish the complete modified source via a public repository;
 * providing source “on request” does NOT satisfy this requirement.
 *
 * See LICENSE (bottom) for full additional terms.
 * See plugin.yml for full notice.
 */


package de.thepixel3261.momentum.lang

import org.bukkit.ChatColor
import java.util.regex.Pattern


object LanguageParser {
    lateinit var languageManager: LanguageManager
    private val PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%")

    fun init(languageManager: LanguageManager) {
        this.languageManager = languageManager
    }

    fun parse(key: String): String {
        return languageManager.langMap[key] ?: ""
    }

    fun String.translate(): String {
        val matcher = PLACEHOLDER_PATTERN.matcher(this)
        val buffer = StringBuffer()

        while (matcher.find()) {
            val match = matcher.group(1)
            val replacement = if (match.startsWith("lang_")) {
                val key = match.substring(5)
                parse(key)
            } else {
                matcher.group()
            }
            matcher.appendReplacement(buffer, replacement)
        }
        matcher.appendTail(buffer)

        // Then parse color codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString())
    }
}