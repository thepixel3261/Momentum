package de.thepixel3261.momentum.util

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import de.thepixel3261.momentum.Main
import org.bukkit.entity.Player

class PlaceholderUtil(private val plugin: Main) : PlaceholderExpansion() {
    override fun getIdentifier(): String = "momentum"
    override fun getAuthor(): String = "thepixel3261"
    override fun getVersion(): String = plugin.description.version
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return ""

        val session = plugin.sessionManager.getSession(player) ?: return "0"

        when (params.lowercase()) {
            "minutes" -> return session.totalPlayMinutes.toString()
            "next_minutes" -> {
                val nextTier = plugin.rewardManager.tiers
                    .filter { !session.claimedTiers.contains(it.id) }
                    .minByOrNull { it.unlockAfterMinutes }

                return if (nextTier != null) {
                    val timeNeeded = nextTier.unlockAfterMinutes - session.totalPlayMinutes
                    if (timeNeeded > 0) timeNeeded.toString() else "Ready!"
                } else {
                    "All claimed!"
                }
            }
            else -> return null
        }
    }
}
