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

import org.bukkit.Material

data class RewardTier(
    val name: String,
    val id: Int,
    val unlockAfterMinutes: Int,
    val actions: List<RewardAction>,
    val materialLocked: Material,
    val materialUnlocked: Material,
    val materialClaimed: Material
)
