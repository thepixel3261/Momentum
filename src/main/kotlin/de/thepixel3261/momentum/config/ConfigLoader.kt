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


package de.thepixel3261.momentum.config

import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.reward.RewardAction
import de.thepixel3261.momentum.reward.RewardManager
import de.thepixel3261.momentum.reward.RewardTier
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigLoader(private val plugin: Main, private val rewardManager: RewardManager) {

    var afkTimeoutMinutes: Int = 5
    var allowClaimAll: Boolean = true
    private var allowIndividualClaim: Boolean = true

    var redisEnabled: Boolean = false
    var redisHost: String = "localhost"
    var redisPort: Int = 6379
    var redisPassword: String = ""

    var guiTitle: String = "Session Rewards"
    var playtimeItemName: String = ""
    var playtimeItemLore: List<String> = emptyList()
    var claimAllItemName: String = ""
    var claimAllItemLore: List<String> = emptyList()
    var tierLockedName: String = ""
    var tierLockedLore: List<String> = emptyList()
    var tierClaimableName: String = ""
    var tierClaimableLore: List<String> = emptyList()
    var tierClaimedName: String = ""
    var tierClaimedLore: List<String> = emptyList()

    fun load() {
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
        allowClaimAll = config.getBoolean("claim.allowClaimAll", true)
        allowIndividualClaim = config.getBoolean("claim.allowIndividualClaim", true)

        redisEnabled = config.getBoolean("redis.enabled", false)
        redisHost = config.getString("redis.host", "localhost") ?: "localhost"
        redisPort = config.getInt("redis.port", 6379)
        redisPassword = config.getString("redis.password", "") ?: ""

        guiTitle = config.getString("gui.title", "Session Rewards") ?: "Session Rewards"
        playtimeItemName = config.getString("gui.playtime_item.name", "") ?: ""
        playtimeItemLore = config.getStringList("gui.playtime_item.lore")
        claimAllItemName = config.getString("gui.claim_all_item.name", "") ?: ""
        claimAllItemLore = config.getStringList("gui.claim_all_item.lore")
        tierLockedName = config.getString("gui.tier_item.locked.name", "") ?: ""
        tierLockedLore = config.getStringList("gui.tier_item.locked.lore")
        tierClaimableName = config.getString("gui.tier_item.claimable.name", "") ?: ""
        tierClaimableLore = config.getStringList("gui.tier_item.claimable.lore")
        tierClaimedName = config.getString("gui.tier_item.claimed.name", "") ?: ""
        tierClaimedLore = config.getStringList("gui.tier_item.claimed.lore")
    }

    private fun loadRewards(rewardsConfig: FileConfiguration) {
        rewardManager.tiers.clear()
        val tiersSection = rewardsConfig.getConfigurationSection("tiers") ?: return

        for (key in tiersSection.getKeys(false)) {
            val tierSection = tiersSection.getConfigurationSection(key) ?: continue
            val id = tierSection.getInt("id")
            val unlockAfterMinutes = tierSection.getInt("unlockAfterMinutes")
            val actionsList = tierSection.getMapList("actions")

            val rewardActions = actionsList.mapNotNull { actionMap ->
                val type = actionMap["type"] as? String ?: return@mapNotNull null
                when (type.lowercase()) {
                    "money" -> RewardAction.GiveMoney((actionMap["amount"] as? Number)?.toDouble() ?: 0.0)
                    "xp" -> RewardAction.GiveXP((actionMap["amount"] as? Int) ?: 0)
                    "command" -> RewardAction.RunCommand(actionMap["command"] as? String ?: "")
                    "sound" -> RewardAction.PlaySound(
                        actionMap["sound"] as? String ?: "",
                        (actionMap["volume"] as? Number)?.toFloat() ?: 1.0f,
                        (actionMap["pitch"] as? Number)?.toFloat() ?: 1.0f
                    )
                    "particle" -> RewardAction.ShowParticle(
                        actionMap["particle"] as? String ?: "",
                        (actionMap["count"] as? Int) ?: 0
                    )
                    else -> null
                }
            }

            rewardManager.tiers.add(RewardTier(id, unlockAfterMinutes, rewardActions))
        }
    }
}
