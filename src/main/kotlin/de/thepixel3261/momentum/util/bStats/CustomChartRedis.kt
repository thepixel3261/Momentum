package de.thepixel3261.momentum.util.bStats

import de.thepixel3261.momentum.Main
import org.bstats.charts.CustomChart
import org.bstats.json.JsonObjectBuilder

class CustomChartRedis (val plugin: Main): CustomChart("redis-use") {
    override fun getChartData(): JsonObjectBuilder.JsonObject {
        return JsonObjectBuilder().appendField("enabled", plugin.configLoader.redisEnabled.toString()).build()
    }
}
