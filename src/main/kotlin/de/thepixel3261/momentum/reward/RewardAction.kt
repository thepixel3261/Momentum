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


package de.thepixel3261.momentum.reward

sealed class RewardAction {
    data class GiveMoney(val amount: Double) : RewardAction()
    data class GiveXP(val amount: Int) : RewardAction()
    data class RunCommand(val command: String) : RewardAction()
    data class PlaySound(val sound: String, val volume: Float, val pitch: Float) : RewardAction()
    data class ShowParticle(val particle: String, val count: Int) : RewardAction()
}
