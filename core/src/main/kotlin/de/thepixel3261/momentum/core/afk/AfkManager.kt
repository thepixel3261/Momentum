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
import de.thepixel3261.momentum.core.util.Scheduler
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class AfkManager(private val plugin: Main, private val sessionManager: SessionManager) {
    fun startAfkChecker() {
        val intervalTicks = 20L * 10

        Scheduler.runTimer({
            for (player in Bukkit.getOnlinePlayers()) {
                handlePlayerAfk(player)
            }
        }, 0L, intervalTicks)
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
            player.sendMessage("%lang_afk.now-afk%".translate())
        } else {
            player.sendMessage("%lang_afk.no-more-afk%".translate())
        }
    }
}
