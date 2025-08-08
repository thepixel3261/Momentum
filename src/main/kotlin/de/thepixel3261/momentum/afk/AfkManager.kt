package de.thepixel3261.momentum.afk

import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.session.SessionManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class AfkManager(private val plugin: Main, private val sessionManager: SessionManager) {

    private val afkPlayers = mutableSetOf<UUID>()

    fun startAfkChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, {
            val afkTimeout = plugin.configLoader.afkTimeoutMinutes
            for (player in Bukkit.getOnlinePlayers()) {
                val session = sessionManager.getSession(player) ?: continue
                val sinceLastActivity = (System.currentTimeMillis() - session.lastActivity) / 60000

                if (sinceLastActivity >= afkTimeout && !session.isAfk) {
                    setAfk(player, true)
                } else if (sinceLastActivity < afkTimeout && session.isAfk) {
                    setAfk(player, false)
                }
            }
        }, 0L, 20L * 10) // Check every 10 seconds
    }

    fun updateActivity(player: Player) {
        val session = sessionManager.getSession(player) ?: return
        session.lastActivity = System.currentTimeMillis()
        if (session.isAfk) {
            setAfk(player, false)
        }
    }

    private fun setAfk(player: Player, afk: Boolean) {
        val session = sessionManager.getSession(player) ?: return
        session.isAfk = afk
        if (afk) {
            afkPlayers.add(player.uniqueId)
            player.sendMessage("You are now AFK.")
        } else {
            afkPlayers.remove(player.uniqueId)
            player.sendMessage("You are no longer AFK.")
        }
    }
}
