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

package de.thepixel3261.momentum.core.session

import java.util.*

data class SessionData(
    val uuid: UUID,
    var joinTime: Long = System.currentTimeMillis(),
    var lastActivity: Long = System.currentTimeMillis(),
    var totalPlayMinutes: Int = 0,
    var claimedTiers: MutableSet<Int> = mutableSetOf(),
    var unlockedTiers: MutableSet<Int> = mutableSetOf(),
    var isAfk: Boolean = false,
    var multiplier: Double = 1.0,
    var lastRecycle: Int = 0, // Playtime-Minutes
    var recycles: Int = 0
)
