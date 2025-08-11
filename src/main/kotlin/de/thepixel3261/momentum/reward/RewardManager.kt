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

package de.thepixel3261.momentum.reward

import de.thepixel3261.momentum.session.SessionManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player

class RewardManager(private val economy: Economy?) {
    lateinit var sessionManager: SessionManager
    val tiers = mutableListOf<RewardTier>()

    fun claimTier(player: Player, tierId: Int) {
        val session = sessionManager.getSession(player) ?: return
        val tier = tiers.find { it.id == tierId } ?: return

        if (!session.unlockedTiers.contains(tierId) || session.claimedTiers.contains(tierId)) {
            player.sendMessage("You can't claim this tier.")
            return
        }

        tier.actions.forEach { action -> executeAction(player, action) }
        session.claimedTiers.add(tierId)
        player.sendMessage("You've claimed the reward for tier $tierId!")
    }

    fun claimAllTiers(player: Player) {
        val session = sessionManager.getSession(player) ?: return
        val claimableTiers = tiers.filter { session.unlockedTiers.contains(it.id) && !session.claimedTiers.contains(it.id) }

        if (claimableTiers.isEmpty()) {
            player.sendMessage("You have no rewards to claim.")
            return
        }

        claimableTiers.forEach { tier ->
            claimTier(player, tier.id)
        }
        player.sendMessage("You've claimed all available rewards!")
    }

    private fun executeAction(player: Player, action: RewardAction) {
        when (action) {
            is RewardAction.GiveMoney -> economy?.depositPlayer(player, action.amount)
            is RewardAction.GiveXP -> player.giveExp(action.amount)
            is RewardAction.RunCommand -> Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                action.command.replace("%player%", player.name)
            )
            is RewardAction.PlaySound -> player.playSound(player.location, Sound.valueOf(action.sound.uppercase()), action.volume, action.pitch)
            is RewardAction.ShowParticle -> player.spawnParticle(Particle.valueOf(action.particle.uppercase()), player.location, action.count)
        }
    }
}
