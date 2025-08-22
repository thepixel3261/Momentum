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
import de.thepixel3261.momentum.core.reward.RewardAction
import de.thepixel3261.momentum.core.session.MultiplierManager
import de.thepixel3261.momentum.core.util.ItemUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.plugin.Plugin

class MomentumMenu(private val plugin: Main) {
    val slots = HashMap<Int, Int>()

    fun open(player: Player) {
        scheduleOnEntity(player) {
            val session = plugin.sessionManager.getSession(player) ?: return@scheduleOnEntity
            val inventory = Bukkit.createInventory(MomentumMenuHolder(), 54, plugin.configLoader.guiTitle)

            // Add placeholder items, info items, etc.
            val playtimeItem = ItemUtil.create(
                Material.CLOCK,
                plugin.configLoader.playtimeItemName,
                plugin.configLoader.playtimeItemLore.map { line ->
                    line.replace("%momentum_minutes%", session.totalPlayMinutes.toString())
                        .replace("%multiplier%", session.multiplier.toString().slice(0..2))
                }
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

            val rewardTiers: MutableSet<Int> = mutableSetOf()
            plugin.rewardManager.tiers.forEach { tier ->
                rewardTiers += tier.id
            }
            if (session.claimedTiers.containsAll(rewardTiers) && plugin.configLoader.allowRecycle) {
                val recycleItem = ItemUtil.create(
                    Material.FIREWORK_ROCKET,
                    "Recycle",
                    listOf(
                        "Multiply %multiplier%x".replace("%multiplier%", MultiplierManager.recycleMultiplier.toString())
                            .replace(
                                "%new_multiplier%",
                                (MultiplierManager.recycleMultiplier * session.multiplier).toString().slice(0..2)
                            ),
                        "New Multiplier: %new_multiplier%x".replace(
                            "%multiplier%",
                            MultiplierManager.recycleMultiplier.toString()
                        ).replace(
                            "%new_multiplier%",
                            (MultiplierManager.recycleMultiplier * session.multiplier).toString().slice(0..2)
                        )
                    ),
                    true
                )
                val meta = recycleItem.itemMeta ?: return@scheduleOnEntity
                meta.itemFlags += ItemFlag.HIDE_ATTRIBUTES
                recycleItem.itemMeta = meta
                inventory.setItem(49, recycleItem)
            }

            // Display reward tiers
            plugin.rewardManager.tiers.sortedBy { it.id }.forEachIndexed { index, tier ->
                val isClaimed = session.claimedTiers.contains(tier.id)
                val isUnlocked = session.unlockedTiers.contains(tier.id)

                val rewardsLore = tier.actions.filter { it.visible }.flatMap { action ->
                    action.lore ?: when (action) {
                        is RewardAction.GiveMoney -> listOf("&e- Money: ${action.amount}")
                        is RewardAction.GiveXP -> listOf("&b- XP: ${action.amount}")
                        is RewardAction.RunCommand -> listOf("&d- Command: /${action.command.split(" ").first()}")
                        is RewardAction.PlaySound -> listOf("&a- Sound Effect")
                        is RewardAction.ShowParticle -> listOf("&c- Particle Effect")
                        else -> {
                            listOf("Custom Action")
                        }
                    }
                }

                val itemStack = when {
                    isClaimed -> ItemUtil.create(
                        Material.GLASS_PANE,
                        plugin.configLoader.tierClaimedName.replace("%tier_id%", tier.id.toString())
                            .replace("%tier_name%", tier.name),
                        plugin.configLoader.tierClaimedLore
                    )

                    isUnlocked -> ItemUtil.create(
                        Material.DIAMOND,
                        plugin.configLoader.tierClaimableName.replace("%tier_id%", tier.id.toString())
                            .replace("%tier_name%", tier.name),
                        plugin.configLoader.tierClaimableLore.flatMap {
                            if (it.contains("%rewards%")) rewardsLore else listOf(it)
                        },
                        glowing = true
                    )

                    else -> {
                        val timeLeft = tier.unlockAfterMinutes - (session.totalPlayMinutes - session.lastRecycle)
                        ItemUtil.create(
                            Material.GRAY_DYE,
                            plugin.configLoader.tierLockedName.replace("%tier_id%", tier.id.toString())
                                .replace("%tier_name%", tier.name),
                            plugin.configLoader.tierLockedLore.flatMap {
                                if (it.contains("%rewards%")) rewardsLore else listOf(it)
                            }.map { it.replace("%time_left%", timeLeft.toString()) })
                    }
                }

                slots[index + 18] = tier.id
                inventory.setItem(18 + index, itemStack)
            }

            player.openInventory(inventory)
        }
    }

    private fun scheduleOnEntity(player: Player, task: () -> Unit) {
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
