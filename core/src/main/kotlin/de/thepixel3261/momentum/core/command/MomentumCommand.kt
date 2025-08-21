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

package de.thepixel3261.momentum.core.command

import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.gui.MomentumMenu
import de.thepixel3261.momentum.core.lang.LanguageManager
import de.thepixel3261.momentum.core.lang.LanguageParser.translate
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class MomentumCommand(private val plugin: Main, private val momentumMenu: MomentumMenu) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("%lang_command.needs-player%".translate())
            return true
        }

        if (args.isEmpty()) {
            momentumMenu.open(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                plugin.loadTime = System.currentTimeMillis()
                if (!sender.hasPermission("momentum.reload")) {
                    sender.sendMessage("%lang_command.no-permission%".translate())
                    return true
                }
                plugin.configLoader.load()
                LanguageManager(plugin)
                sender.sendMessage("%lang_command.reloaded%".translate())
                plugin.logger.info(plugin.startUpLog().joinToString("\n"))
            }
            "info" -> {
                val session = plugin.sessionManager.getSession(sender) ?: return true
                val currentTier = plugin.rewardManager.tiers
                    .filter { session.unlockedTiers.contains(it.id) }
                    .maxByOrNull { it.unlockAfterMinutes }
                
                sender.sendMessage("&a=== [Momentum Info] ===".translate())
                sender.sendMessage("&7Multiplier: &f${session.multiplier}".translate())
                sender.sendMessage("&7Playtime: &f${session.totalPlayMinutes} minutes".translate())
                sender.sendMessage("&7Recycle Count: &f${session.recycles}".translate())
                sender.sendMessage("&7Current Tier: &f${currentTier?.name ?: "None"}".translate())
                return true
            }
            else -> momentumMenu.open(sender)
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): MutableList<String> {
        if (sender !is Player) return mutableListOf()
        if (args?.size != 1) return mutableListOf()
        val completions = mutableListOf("info")
        if (sender.hasPermission("momentum.reload")) completions.add("reload")
        return completions
    }
}
