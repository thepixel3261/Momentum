package de.thepixel3261.momentum.util.bStats

import de.thepixel3261.momentum.Main
import org.bstats.bukkit.Metrics

class BStatsUtil(val plugin: Main) {
    private val bStatsId = 26832
    val metrics: Metrics = Metrics(plugin, bStatsId)

    init {
        metrics.addCustomChart(CustomChartRedis(plugin))
    }
}
