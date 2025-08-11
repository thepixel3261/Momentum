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

package de.thepixel3261.momentum.util

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import de.thepixel3261.momentum.Main
import org.bukkit.entity.Player

class PlaceholderUtil(private val plugin: Main) : PlaceholderExpansion() {
    override fun getIdentifier(): String = "momentum"
    override fun getAuthor(): String = "thepixel3261"
    override fun getVersion(): String = plugin.description.version
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return ""

        val session = plugin.sessionManager.getSession(player) ?: return "0"

        when (params.lowercase()) {
            "minutes" -> return session.totalPlayMinutes.toString()
            "next_minutes" -> {
                val nextTier = plugin.rewardManager.tiers
                    .filter { !session.claimedTiers.contains(it.id) }
                    .minByOrNull { it.unlockAfterMinutes }

                return if (nextTier != null) {
                    val timeNeeded = nextTier.unlockAfterMinutes - session.totalPlayMinutes
                    if (timeNeeded > 0) timeNeeded.toString() else "Ready!"
                } else {
                    "All claimed!"
                }
            }
            else -> return null
        }
    }
}
