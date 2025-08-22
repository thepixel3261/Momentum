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

package de.thepixel3261.momentum.core.session

import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.redis.RedisManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin

class SessionListener(
    private val plugin: Main,
    private val sessionManager: SessionManager,
    private val redisManager: RedisManager
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        // Async check for server switch and load session data if present
        redisManager.isLeavingAsync(player.uniqueId) { isServerSwitch ->
            if (!isServerSwitch) {
                // No cross-server switch; start new session on the player's entity scheduler
                scheduleOnEntity(player) {
                    sessionManager.startSession(player)
                }
                // Ensure any stale keys are cleared in background
                redisManager.clearLeavingAsync(player.uniqueId)
                redisManager.clearSessionDataAsync(player.uniqueId)
            } else {
                redisManager.getSessionDataAsync(player.uniqueId) { sessionData ->
                    scheduleOnEntity(player) {
                        if (sessionData != null) {
                            sessionManager.getSession(player) ?: sessionManager.startSession(player, sessionData)
                        } else {
                            sessionManager.startSession(player)
                        }
                    }
                    // Cleanup keys asynchronously
                    redisManager.clearLeavingAsync(player.uniqueId)
                    redisManager.clearSessionDataAsync(player.uniqueId)
                }
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (redisManager.jedisPool != null) {
            sessionManager.getSession(player)?.let { data ->
                redisManager.setLeavingAsync(data)
            }
        }
        sessionManager.endSession(player)
    }

    private fun scheduleOnEntity(player: org.bukkit.entity.Player, task: () -> Unit) {
        try {
            val getScheduler = player.javaClass.getMethod("getScheduler")
            val scheduler = getScheduler.invoke(player)
            val runMethod = scheduler.javaClass.getMethod(
                "run",
                Plugin::class.java,
                Runnable::class.java,
                Runnable::class.java,
                java.lang.Long.TYPE
            )
            runMethod.invoke(scheduler, plugin, Runnable { task() }, null, 0L)
        } catch (_: Throwable) {
            if (Bukkit.isPrimaryThread()) task() else Bukkit.getScheduler().runTask(plugin, Runnable { task() })
        }
    }
}
