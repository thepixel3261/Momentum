package de.thepixel3261.momentum.session

import de.thepixel3261.momentum.redis.RedisManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class SessionListener(
    private val sessionManager: SessionManager,
    private val redisManager: RedisManager
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val isServerSwitch = redisManager.isLeaving(player.uniqueId)
        val sessionData = if (isServerSwitch) {
            redisManager.getSessionData(player.uniqueId)
        } else {
            null
        }
        redisManager.clearLeaving(player.uniqueId)
        redisManager.clearSessionData(player.uniqueId)

        if (sessionData != null) {
            // Update the session with the stored data
            val session = sessionManager.getSession(player) ?: sessionManager.startSession(player, null)

            session.joinTime = sessionData.joinTime
            session.totalPlayMinutes = sessionData.totalPlayMinutes
            session.claimedTiers = sessionData.claimedTiers
            session.unlockedTiers = sessionData.unlockedTiers
        } else {
            sessionManager.startSession(player, null)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (redisManager.jedisPool != null) { // only set leaving if redis is enabled
            redisManager.setLeaving(sessionManager.getSession(player)!!)
        }
        sessionManager.endSession(player)
    }
}
