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


package de.thepixel3261.momentum.gui

import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.reward.RewardAction
import de.thepixel3261.momentum.util.ItemUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

class MomentumMenu(private val plugin: Main) {
    fun open(player: Player) {
        val session = plugin.sessionManager.getSession(player) ?: return
        val inventory = Bukkit.createInventory(null, 54, plugin.configLoader.guiTitle)

        // Add placeholder items, info items, etc.
        val playtimeItem = ItemUtil.create(
            Material.CLOCK,
            plugin.configLoader.playtimeItemName,
            plugin.configLoader.playtimeItemLore.map { it.replace("%momentum_minutes%", session.totalPlayMinutes.toString()) }
        )
        inventory.setItem(4, playtimeItem)

        if (plugin.configLoader.allowClaimAll) {
            val claimAllItem = ItemUtil.create(
                Material.CHEST,
                plugin.configLoader.claimAllItemName,
                plugin.configLoader.claimAllItemLore
            )
            inventory.setItem(49, claimAllItem)
        }

        // Display reward tiers
        plugin.rewardManager.tiers.sortedBy { it.unlockAfterMinutes }.forEachIndexed { index, tier ->
            val isClaimed = session.claimedTiers.contains(tier.id)
            val isUnlocked = session.unlockedTiers.contains(tier.id)

            val rewardsLore = tier.actions.map { action ->
                when(action) {
                    is RewardAction.GiveMoney -> "&e- Money: ${action.amount}"
                    is RewardAction.GiveXP -> "&b- XP: ${action.amount}"
                    is RewardAction.RunCommand -> "&d- Command: /${action.command.split(" ").first()}"
                    is RewardAction.PlaySound -> "&a- Sound Effect"
                    is RewardAction.ShowParticle -> "&c- Particle Effect"
                }
            }

            val itemStack = when {
                isClaimed -> ItemUtil.create(Material.GLASS_PANE, plugin.configLoader.tierClaimedName.replace("%tier_id%", tier.id.toString()), plugin.configLoader.tierClaimedLore)
                isUnlocked -> ItemUtil.create(Material.DIAMOND, plugin.configLoader.tierClaimableName.replace("%tier_id%", tier.id.toString()), plugin.configLoader.tierClaimableLore.flatMap { if (it.contains("%rewards%")) rewardsLore else listOf(it) }, glowing = true)
                else -> {
                    val timeLeft = tier.unlockAfterMinutes - session.totalPlayMinutes
                    ItemUtil.create(Material.GRAY_DYE, plugin.configLoader.tierLockedName.replace("%tier_id%", tier.id.toString()), plugin.configLoader.tierLockedLore.flatMap { if (it.contains("%rewards%")) rewardsLore else listOf(it) }.map { it.replace("%time_left%", timeLeft.toString()) })
                }
            }
            inventory.setItem(18 + index, itemStack)
        }

        player.openInventory(inventory)
    }
}
