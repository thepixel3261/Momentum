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

package de.thepixel3261.momentum.session

import de.thepixel3261.momentum.redis.RedisManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class SessionListener(
    private val sessionManager: SessionManager,
    private val redisManager: RedisManager
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val isServerSwitch = redisManager.isLeaving(player.uniqueId)
        val sessionData = if (isServerSwitch) {
            redisManager.getSessionData(player.uniqueId)
        } else {
            null
        }
        redisManager.clearLeaving(player.uniqueId)
        redisManager.clearSessionData(player.uniqueId)

        if (sessionData != null) {
            // Update the session with the stored data
            sessionManager.getSession(player) ?: sessionManager.startSession(player, sessionData)
        } else {
            sessionManager.startSession(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (redisManager.jedisPool != null) {
            redisManager.setLeaving(sessionManager.getSession(player)!!)
        }
        sessionManager.endSession(player)
    }
}
