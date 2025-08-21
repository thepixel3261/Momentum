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


package de.thepixel3261.momentum.core.util

import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.session.MultiplierManager
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie
import org.bstats.charts.SingleLineChart
import org.bstats.charts.DrilldownPie

class BstatsUtil(val plugin: Main) {
    private val bStatsId = 26832
    val metrics = Metrics(plugin, bStatsId)

    init {
        metrics.addCustomChart(SimplePie("redis_use") {
            return@SimplePie plugin.configLoader.redisEnabled.toString()
        })
        metrics.addCustomChart(SimplePie("tiers_count") {
            return@SimplePie plugin.configLoader.tierCount.toString()
        })
        metrics.addCustomChart(SimplePie("amount_of_players") {
            return@SimplePie plugin.server.onlinePlayers.size.toString()
        })
        metrics.addCustomChart(SingleLineChart("tiers_claimed_30") {
            val amount = plugin.rewardManager.claimedTiers30
            plugin.rewardManager.claimedTiers30 = 0
            return@SingleLineChart amount
        })
        metrics.addCustomChart(SingleLineChart("tiers_claimed") {
            return@SingleLineChart plugin.rewardManager.claimedTiers
        })
        metrics.addCustomChart(SingleLineChart("actions_executed_30") {
            val amount = plugin.rewardManager.executedActions30
            plugin.rewardManager.executedActions30 = 0
            return@SingleLineChart amount
        })
        metrics.addCustomChart(SingleLineChart("actions_executed") {
            return@SingleLineChart plugin.rewardManager.executedActions
        })
        metrics.addCustomChart(DrilldownPie("dependencies") {
            val map = HashMap<String, Map<String, Int>>()
            if (plugin.placeholderAPIv != null) {
                map["placeholderapi"] = mapOf(plugin.placeholderAPIv!! to 1)
            }
            if (plugin.vaultv != null) {
                map["vault"] = mapOf(plugin.vaultv!! to 1)
            }

            return@DrilldownPie map
        })
        metrics.addCustomChart(SimplePie("language") {
            val language = plugin.configLoader.lang
            return@SimplePie language
        })
        metrics.addCustomChart(SingleLineChart("recycles_30") {
            val amount = MultiplierManager.recycles30
            return@SingleLineChart amount
        })
        metrics.addCustomChart(SingleLineChart("recycles") {
            val amount = MultiplierManager.recycles
            return@SingleLineChart amount
        })
    }
}
