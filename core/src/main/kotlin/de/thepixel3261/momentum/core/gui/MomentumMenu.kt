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
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.net.URI
import java.util.*
import kotlin.math.ceil

class MomentumMenu(private val plugin: Main) {
    val slots = HashMap<Int, HashMap<Int, Int>>()

    fun open(player: Player, page: Int = 1) {
        val session = plugin.sessionManager.getSession(player) ?: return
        val inventory = Bukkit.createInventory(
            MomentumMenuHolder(page), 54, plugin.configLoader.guiTitle
                .replace("%page%", page.toString())
        )

        // Add placeholder items, info items, etc.
        val playtimeItem = ItemUtil.create(
            Material.valueOf(plugin.configLoader.playtimeItemMaterial),
            plugin.configLoader.playtimeItemName,
            plugin.configLoader.playtimeItemLore.map { line ->
                line.replace("%momentum_minutes%", session.totalPlayMinutes.toString())
                    .replace("%multiplier%", session.multiplier.toString().slice(0..2))
            }
        )
        inventory.setItem(4, playtimeItem)

        if (plugin.configLoader.allowClaimAll) {
            val claimAllItem = ItemUtil.create(
                Material.valueOf(plugin.configLoader.claimAllItemMaterial),
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
                Material.valueOf(plugin.configLoader.recycleItemMaterial),
                plugin.configLoader.recycleItemName,
                plugin.configLoader.recycleItemLore.map { line ->
                    line.replace("%multiplier%", session.multiplier.toString())
                        .replace(
                            "%new_multiplier%",
                            (MultiplierManager.recycleMultiplier * session.multiplier).toString().slice(0..2)
                        )
                },
                true
            )
            val meta = recycleItem.itemMeta ?: return
            meta.itemFlags += ItemFlag.HIDE_ATTRIBUTES
            recycleItem.itemMeta = meta
            inventory.setItem(49, recycleItem)
        }

        // Page Navigators
        if (ceil(rewardTiers.size / 18.0).toInt() >= page + 1) {
            val pageUpItem = if (plugin.configLoader.pageUpMaterial == "DEFAULT" || plugin.configLoader.pageUpMaterial.isEmpty()) {
                getCustomSkull("http://textures.minecraft.net/texture/7c69d41076a8dea4f06d3f1a9ac47cc996988b74a0913ab2ac1a74caf7081918")
            } else {
                ItemStack(Material.valueOf(plugin.configLoader.pageUpMaterial))
            }

            val pageUpMeta = pageUpItem.itemMeta

            pageUpMeta?.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.configLoader.pageUpName))
            pageUpMeta?.lore = plugin.configLoader.pageUpLore.map { line ->
                ChatColor.translateAlternateColorCodes('&', line)
            }

            pageUpItem.itemMeta = pageUpMeta
            inventory.setItem(53, pageUpItem)
        }
        if (page > 1) {
            val pageDownItem = if (plugin.configLoader.pageDownMaterial == "DEFAULT" || plugin.configLoader.pageDownMaterial.isEmpty()) {
                getCustomSkull("http://textures.minecraft.net/texture/de1dfc11a837111d22b001a14461f9a7fc093522f88c58faefd6adeffcd4e9ab")
            } else {
                ItemStack(Material.valueOf(plugin.configLoader.pageDownMaterial))
            }

            val pageDownMeta = pageDownItem.itemMeta

            pageDownMeta?.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.configLoader.pageDownName))
            pageDownMeta?.lore = plugin.configLoader.pageDownLore.map { line ->
                ChatColor.translateAlternateColorCodes('&', line)
            }

            pageDownItem.itemMeta = pageDownMeta

            inventory.setItem(52, pageDownItem)
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
                        }.map { it.replace("%time_left%", timeLeft.toString()) }
                    )
                }
            }

            val pageToSet = ceil((index + 1) / 18.0).toInt()
            slots[pageToSet] = hashMapOf()
            if (pageToSet == page) {
                val slotToSet = 35 + ((index + 1) - pageToSet * 18)
                (slots[pageToSet])!![slotToSet] = tier.id
                (inventory.holder as MomentumMenuHolder).setTier(slotToSet, tier.id)
                inventory.setItem(slotToSet, itemStack)
            }
        }

        player.openInventory(inventory)
    }

    private fun getCustomSkull(url: String): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        if (url.isEmpty()) return head

        try {
            // Create a player profile
            val profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "MomentumHead")
            val textures = profile.textures
            textures.skin = URI(url).toURL()
            profile.setTextures(textures)

            // Apply the profile to the head
            val meta = head.itemMeta as? SkullMeta ?: return head
            meta.ownerProfile = profile
            head.itemMeta = meta
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return head
    }
}
