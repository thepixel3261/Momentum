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

package de.thepixel3261.momentum.core.reward

import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.lang.LanguageParser.translate
import de.thepixel3261.momentum.core.session.SessionManager
import de.thepixel3261.momentum.api.MomentumAPI
import de.thepixel3261.momentum.api.RewardActionContext
import de.thepixel3261.momentum.api.SessionSnapshot
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

        // Try custom/override executor first
        fun tryExecuteById(id: String, params: Map<String, Any>, visible: Boolean, lore: List<String>?): Boolean {
            val exec = MomentumAPI.get()?.rewards()?.executorFor(id.lowercase()) ?: return false
            val snap = SessionSnapshot(
                uuid = session.uuid,
                joinTime = session.joinTime,
                lastActivity = session.lastActivity,
                totalPlayMinutes = session.totalPlayMinutes,
                claimedTiers = session.claimedTiers.toSet(),
                unlockedTiers = session.unlockedTiers.toSet(),
                isAfk = session.isAfk,
                multiplier = session.multiplier,
                lastRecycle = session.lastRecycle,
                recycles = session.recycles,
            )
            exec.execute(RewardActionContext(player.uniqueId, snap, params, visible, lore, multiplier))
            return true
        }

        when (action) {
            is RewardAction.GiveMoney -> if (!tryExecuteById("money", mapOf("amount" to action.amount), action.visible, action.lore)) {
                economy?.depositPlayer(player, action.amount * multiplier)
            }
            is RewardAction.GiveXP -> if (!tryExecuteById("xp", mapOf("amount" to action.amount), action.visible, action.lore)) {
                player.giveExp((action.amount * multiplier).toInt())
            }
            is RewardAction.RunCommand -> if (!tryExecuteById("command", mapOf("command" to action.command, "amount" to action.amount), action.visible, action.lore)) {
                Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                action.command.replace("%player%", player.name)
                    .replace("%amount%", (action.amount * multiplier).toString())
                    .replace("%amountR%", (action.amount * multiplier).toInt().toString())
                )
            }
            is RewardAction.PlaySound -> if (!tryExecuteById("sound", mapOf("sound" to action.sound, "volume" to action.volume, "pitch" to action.pitch), action.visible, action.lore)) {
                player.playSound(player.location, Sound.valueOf(action.sound.uppercase()), action.volume, action.pitch)
            }
            is RewardAction.ShowParticle -> if (!tryExecuteById("particle", mapOf("particle" to action.particle, "count" to action.count), action.visible, action.lore)) {
                player.spawnParticle(Particle.valueOf(action.particle.uppercase()), player.location, action.count)
            }
            is RewardAction.Custom -> {
                if (!tryExecuteById(action.id, action.params, action.visible, action.lore)) {
                    plugin.logger.warning("No executor registered for custom reward action id '${action.id}'. Skipping.")
                }
            }
        }
    }
}
