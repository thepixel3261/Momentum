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


package de.thepixel3261.momentum.util

import de.thepixel3261.momentum.Main
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie

class BstatsUtil(val plugin: Main) {
    private val bStatsId = 26832
    val metrics = Metrics(plugin, bStatsId)

    init {
        metrics.addCustomChart(SimplePie("redis_use") {
            return@SimplePie plugin.configLoader.redisEnabled.toString()
        })
    }
}
