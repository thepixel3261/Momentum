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
 * See LICENSE (bottom) for full terms.
 */

package de.thepixel3261.momentum.session

import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.reward.RewardManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class SessionManager(private val plugin: Main) {
    lateinit var rewardManager: RewardManager
    private val sessions = mutableMapOf<UUID, SessionData>()

    fun startSession(player: Player, serverSwitch: SessionData?): SessionData {
        val session: SessionData = serverSwitch
            ?: SessionData(
                uuid = player.uniqueId,
                joinTime = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis(),
                totalPlayMinutes = 0,
                claimedTiers = mutableSetOf(),
                unlockedTiers = mutableSetOf(),
                isAfk = false
            )
        sessions[player.uniqueId] = session
        return session
    }

    fun endSession(player: Player) {
        sessions.remove(player.uniqueId)
    }

    fun getSession(player: Player): SessionData? {
        return sessions[player.uniqueId]
    }

    fun startRewardChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, {
            for (player in Bukkit.getOnlinePlayers()) {
                val session = getSession(player) ?: continue
                if (session.isAfk) continue

                session.totalPlayMinutes++

                rewardManager.tiers.forEach { tier ->
                    if (session.totalPlayMinutes >= tier.unlockAfterMinutes && !session.unlockedTiers.contains(tier.id)) {
                        session.unlockedTiers.add(tier.id)
                        player.sendMessage("You've unlocked a new reward tier!")
                    }
                }
            }
        }, 0L, 20L * 60)
    }
}
