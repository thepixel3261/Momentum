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

package de.thepixel3261.momentum.core.afk

import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.lang.LanguageParser.translate
import de.thepixel3261.momentum.core.session.SessionManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.function.Consumer

class AfkManager(private val plugin: Main, private val sessionManager: SessionManager) {

    private val afkPlayers = mutableSetOf<UUID>()

    fun startAfkChecker() {
        val isFolia = try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
            true
        } catch (_: ClassNotFoundException) {
            false
        }

        val intervalTicks = 20L * 10 // 10 seconds

        if (isFolia) {
            try {
                val getGrsMethod = Bukkit::class.java.getMethod("getGlobalRegionScheduler")
                val grs = getGrsMethod.invoke(null)
                val runAtFixedRate = grs.javaClass.getMethod(
                    "runAtFixedRate",
                    Plugin::class.java,
                    Consumer::class.java,
                    java.lang.Long.TYPE,
                    java.lang.Long.TYPE
                )

                runAtFixedRate.invoke(
                    grs,
                    plugin,
                    Consumer<Any> { _ ->
                        for (player in Bukkit.getOnlinePlayers()) {
                            scheduleOnEntity(player) {
                                handlePlayerAfk(player)
                            }
                        }
                    },
                    0L,
                    intervalTicks
                )
            } catch (t: Throwable) {
                plugin.logger.warning("Folia scheduling failed, falling back to Bukkit scheduler: ${t.message}")
                Bukkit.getScheduler().runTaskTimer(plugin, {
                    for (player in Bukkit.getOnlinePlayers()) {
                        handlePlayerAfk(player)
                    }
                }, 0L, intervalTicks)
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, {
                for (player in Bukkit.getOnlinePlayers()) {
                    handlePlayerAfk(player)
                }
            }, 0L, intervalTicks)
        }
    }

    private fun handlePlayerAfk(player: Player) {
        val session = sessionManager.getSession(player) ?: return
        val afkTimeout = plugin.configLoader.afkTimeoutMinutes
        val sinceLastActivity = (System.currentTimeMillis() - session.lastActivity) / 60000

        if (sinceLastActivity >= afkTimeout && !session.isAfk) {
            setAfk(player, true)
        } else if (sinceLastActivity < afkTimeout && session.isAfk) {
            setAfk(player, false)
        }
    }

    private fun scheduleOnEntity(player: Player, task: () -> Unit) {
        try {
            val getScheduler = player.javaClass.getMethod("getScheduler")
            val scheduler = getScheduler.invoke(player)
            val runMethod = scheduler.javaClass.getMethod(
                "run",
                Plugin::class.java,
                Runnable::class.java,
                Runnable::class.java,
                java.lang.Long.TYPE
            )
            runMethod.invoke(scheduler, plugin, Runnable { task() }, null, 0L)
        } catch (_: Throwable) {
            task()
        }
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
            player.sendMessage("%lang_afk.now-afk%".translate())
        } else {
            afkPlayers.remove(player.uniqueId)
            player.sendMessage("%lang_afk.no-more-afk%".translate())
        }
    }
}
