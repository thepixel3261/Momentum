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

package de.thepixel3261.momentum.core.config

import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.reward.RewardAction
import de.thepixel3261.momentum.core.reward.RewardManager
import de.thepixel3261.momentum.core.reward.RewardTier
import de.thepixel3261.momentum.core.session.MultiplierManager
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigLoader(private val plugin: Main, private val rewardManager: RewardManager) {

    var afkTimeoutMinutes: Int = 5
    var allowClaimAll: Boolean = true
    var allowIndividualClaim: Boolean = true
    var allowRecycle: Boolean = true

    var lang: String = "en_US"

    var redisEnabled: Boolean = false
    var redisHost: String = "localhost"
    var redisPort: Int = 6379
    var redisUser: String = ""
    var redisPassword: String = ""
    var redisSsl: Boolean = false
    var redisSslVerifyPeer: Boolean = true

    var guiTitle: String = "Session Rewards"
    var playtimeItemName: String = ""
    var playtimeItemLore: List<String> = emptyList()
    var claimAllItemName: String = ""
    var claimAllItemLore: List<String> = emptyList()
    var recycleItemName: String = ""
    var recycleItemLore: List<String> = emptyList()
    var tierLockedName: String = ""
    var tierLockedLore: List<String> = emptyList()
    var tierClaimableName: String = ""
    var tierClaimableLore: List<String> = emptyList()
    var tierClaimedName: String = ""
    var tierClaimedLore: List<String> = emptyList()

    var tierCount: Int = 0

    fun load() {
        tierCount = 0
        plugin.saveDefaultConfig()
        loadMainConfig(plugin.config)

        val rewardsFile = File(plugin.dataFolder, "rewards.yml")
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false)
        }
        loadRewards(YamlConfiguration.loadConfiguration(rewardsFile))
    }

    private fun loadMainConfig(config: FileConfiguration) {
        afkTimeoutMinutes = config.getInt("afkTimeoutMinutes", 5)
        lang = config.getString("language", "en_US")
        allowClaimAll = config.getBoolean("claim.allowClaimAll", true)
        allowIndividualClaim = config.getBoolean("claim.allowIndividualClaim", true)
        allowRecycle = config.getBoolean("claim.recycle.enabled", true)
        MultiplierManager.recycleMultiplier = (config.getDouble("claim.recycle.recycleMultiplier", 1.0))

        redisEnabled = config.getBoolean("redis.enabled", false)
        redisHost = config.getString("redis.host", "localhost") ?: "localhost"
        redisPort = config.getInt("redis.port", 6379)
        redisUser = config.getString("redis.user", "")
        redisPassword = config.getString("redis.password", "") ?: ""
        redisSsl = config.getBoolean("redis.ssl", false)
        redisSslVerifyPeer = config.getBoolean("redis.ssl_verify_peer", true)

        guiTitle = config.getString("gui.title", "Session Rewards")
        playtimeItemName = config.getString("gui.playtime_item.name", "")
        playtimeItemLore = config.getStringList("gui.playtime_item.lore")
        claimAllItemName = config.getString("gui.claim_all_item.name", "")
        claimAllItemLore = config.getStringList("gui.claim_all_item.lore")
        recycleItemName = config.getString("gui.recycle_item.name", "")
        recycleItemLore = config.getStringList("gui.recycle_item.lore")
        tierLockedName = config.getString("gui.tier_item.locked.name", "")
        tierLockedLore = config.getStringList("gui.tier_item.locked.lore")
        tierClaimableName = config.getString("gui.tier_item.claimable.name", "")
        tierClaimableLore = config.getStringList("gui.tier_item.claimable.lore")
        tierClaimedName = config.getString("gui.tier_item.claimed.name", "")
        tierClaimedLore = config.getStringList("gui.tier_item.claimed.lore")
    }

    private fun loadRewards(rewardsConfig: FileConfiguration) {
        rewardManager.tiers.clear()
        val tiersSection = rewardsConfig.getConfigurationSection("tiers") ?: return

        for (tierName in tiersSection.getKeys(false)) {
            tierCount++
            val tierSection = tiersSection.getConfigurationSection(tierName) ?: continue
            val id = tierSection.getInt("id")
            val unlockAfterMinutes = tierSection.getInt("unlockAfterMinutes")
            val actionsList = tierSection.getMapList("actions")
            val materialLocked = Material.valueOf(tierSection.getString("materialLocked", "GRAY_DYE").uppercase())
            val materialUnlocked = Material.valueOf(tierSection.getString("material", "DIAMOND").uppercase())
            val materialClaimed = Material.valueOf(tierSection.getString("materialClaimed", "GLASS_PANE").uppercase())

            val rewardActions = actionsList.mapNotNull { actionMap ->
                val type = actionMap["type"] as? String ?: return@mapNotNull null
                val visible = actionMap["visible"] as? Boolean ?: true
                val lore = actionMap["lore"] as? List<String>

                when (type.lowercase()) {
                    "money" -> RewardAction.GiveMoney((actionMap["amount"] as? Number)?.toDouble() ?: 0.0, visible, lore)
                    "xp" -> RewardAction.GiveXP((actionMap["amount"] as? Int) ?: 0, visible, lore)
                    "command" -> RewardAction.RunCommand(actionMap["command"] as? String ?: "", actionMap["amount"] as? Double ?: 1.0,visible, lore)
                    "sound" -> RewardAction.PlaySound(
                        actionMap["sound"] as? String ?: "",
                        (actionMap["volume"] as? Number)?.toFloat() ?: 1.0f,
                        (actionMap["pitch"] as? Number)?.toFloat() ?: 1.0f,
                        visible,
                        lore
                    )
                    "particle" -> RewardAction.ShowParticle(
                        actionMap["particle"] as? String ?: "",
                        (actionMap["count"] as? Int) ?: 0,
                        visible,
                        lore
                    )
                    else -> RewardAction.Custom(type.lowercase(), actionMap, visible, lore)
                }
            }

            rewardManager.tiers.add(RewardTier(tierName, id, unlockAfterMinutes, rewardActions, materialLocked, materialUnlocked, materialClaimed))
        }
    }
}
