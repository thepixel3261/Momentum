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
 * See LICENSE (bottom) for full terms.
 */


package de.thepixel3261.momentum.command

import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.gui.MomentumMenu
import de.thepixel3261.momentum.reward.RewardManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MomentumCommand(private val plugin: Main, private val rewardManager: RewardManager, private val momentumMenu: MomentumMenu) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be run by a player.")
            return true
        }

        if (args.isEmpty()) {
            momentumMenu.open(sender)
            return true
        }

        when (args[0].lowercase()) {
            "claim" -> {
                if (args.size == 1) {
                    sender.sendMessage("Usage: /momentum claim <tierId|all>")
                    return true
                }
                when (args[1].lowercase()) {
                    "all" -> rewardManager.claimAllTiers(sender)
                    else -> {
                        val tierId = args[1].toIntOrNull()
                        if (tierId == null) {
                            sender.sendMessage("Invalid tier ID.")
                        } else {
                            rewardManager.claimTier(sender, tierId)
                        }
                    }
                }
            }
            "reload" -> {
                if (!sender.hasPermission("momentum.reload")) {
                    sender.sendMessage("You don't have permission to do that.")
                    return true
                }
                plugin.configLoader.load()
                sender.sendMessage("Momentum config reloaded.")
            }
            else -> momentumMenu.open(sender)
        }

        return true
    }
}
