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

package de.thepixel3261.momentum.core.gui

import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.lang.LanguageParser.translate
import de.thepixel3261.momentum.core.session.MultiplierManager.recycle
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class GuiListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory.holder !is MomentumMenuHolder) return

        event.isCancelled = true

        val player = event.whoClicked as? org.bukkit.entity.Player ?: return

        // Recycle button
        val session = plugin.sessionManager.getSession(player) ?: plugin.sessionManager.startSession(player)
        val rewardTiers: MutableSet<Int> = mutableSetOf()
        plugin.rewardManager.tiers.forEach { tier ->
            rewardTiers += tier.id
        }
        if (session.claimedTiers.containsAll(rewardTiers) && plugin.configLoader.allowRecycle) {
            if (event.slot == 49) {
                session.recycle()
                player.closeInventory()
                player.sendMessage("%lang_claim.recycle-success%".translate())
                return
            }
        }

        // Claim All button
        if (event.slot == 49) {
            plugin.rewardManager.claimAllTiers(player)
            player.closeInventory()
            return
        }

        // Tier items
        val tierId = plugin.momentumMenu.slots[event.slot] ?: return

        plugin.rewardManager.claimTier(player, tierId)
        plugin.momentumMenu.open(player) // Refresh GUI
    }
}
