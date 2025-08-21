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

sealed class RewardAction {
    abstract val visible: Boolean
    abstract val lore: List<String>?

    data class GiveMoney(val amount: Double, override val visible: Boolean, override val lore: List<String>?) : RewardAction()
    data class GiveXP(val amount: Int, override val visible: Boolean, override val lore: List<String>?) : RewardAction()
    data class RunCommand(val command: String, val amount: Double, override val visible: Boolean, override val lore: List<String>?) : RewardAction()
    data class PlaySound(val sound: String, val volume: Float, val pitch: Float, override val visible: Boolean, override val lore: List<String>?) : RewardAction()
    data class ShowParticle(val particle: String, val count: Int, override val visible: Boolean, override val lore: List<String>?) : RewardAction()
    data class Custom(val id: String, val params: MutableMap<String, Any>, override val visible: Boolean, override val lore: List<String>?) : RewardAction()
}
