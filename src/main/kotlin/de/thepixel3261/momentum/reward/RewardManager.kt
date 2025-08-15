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

import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.lang.LanguageParser.translate
import de.thepixel3261.momentum.session.SessionManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player

class RewardManager(private val economy: Economy?, val plugin: Main) {
    lateinit var sessionManager: SessionManager
    val tiers = mutableListOf<RewardTier>()
    var claimedTiers30 = 0
    var claimedTiers = 0
    var executedActions30 = 0
    var executedActions = 0

    fun claimTier(player: Player, tierId: Int, claimAll: Boolean = false) {
        claimedTiers30++
        claimedTiers++

        if (!plugin.configLoader.allowIndividualClaim && !claimAll) {
            player.sendMessage("%lang_claim.individual-claim-disabled%".translate())
            return
        }

        val session = sessionManager.getSession(player) ?: return
        val tier = tiers.find { it.id == tierId } ?: return

        if (!session.unlockedTiers.contains(tierId) || session.claimedTiers.contains(tierId)) {
            player.sendMessage("%lang_claim.cannot-claim%".translate())
            return
        }

        tier.actions.forEach { action -> executeAction(player, action) }
        session.claimedTiers.add(tierId)
        player.sendMessage("%lang_claim.success%".translate().replace("%tier_name%", tier.name))
    }

    fun claimAllTiers(player: Player) {
        val session = sessionManager.getSession(player) ?: return
        val claimableTiers = tiers.filter { session.unlockedTiers.contains(it.id) && !session.claimedTiers.contains(it.id) }

        if (claimableTiers.isEmpty()) {
            player.sendMessage("%lang_claim.no-claimable-tiers%".translate())
            return
        }

        claimableTiers.forEach { tier ->
            claimTier(player, tier.id, true)
        }
        player.sendMessage("%lang_claim.all-success%".translate())
    }

    private fun executeAction(player: Player, action: RewardAction) {
        executedActions++
        executedActions30++

        val session = sessionManager.getSession(player) ?: sessionManager.startSession(player)
        val multiplier = session.multiplier

        when (action) {
            is RewardAction.GiveMoney -> economy?.depositPlayer(player, action.amount * multiplier)
            is RewardAction.GiveXP -> player.giveExp(action.amount * multiplier.toInt())
            is RewardAction.RunCommand -> Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                action.command.replace("%player%", player.name)
                    .replace("%amount%", (action.amount * multiplier).toString())
                    .replace("%amountR%", (action.amount * multiplier).toInt().toString())
            )
            is RewardAction.PlaySound -> player.playSound(player.location, Sound.valueOf(action.sound.uppercase()), action.volume, action.pitch)
            is RewardAction.ShowParticle -> player.spawnParticle(Particle.valueOf(action.particle.uppercase()), player.location, action.count)
        }
    }
}
