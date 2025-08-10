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

package de.thepixel3261.momentum.afk

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.AsyncPlayerChatEvent

class AfkListener(private val afkManager: AfkManager) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event.from.x != event.to.x || event.from.y != event.to.y || event.from.z != event.to.z) {
            afkManager.updateActivity(event.player)
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        afkManager.updateActivity(event.player)
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        afkManager.updateActivity(event.player)
    }
}
