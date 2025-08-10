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

package de.thepixel3261.momentum.session

import java.util.*

data class SessionData(
    val uuid: UUID,
    var joinTime: Long,
    var lastActivity: Long,
    var totalPlayMinutes: Int,
    var claimedTiers: MutableSet<Int>,
    var unlockedTiers: MutableSet<Int>,
    var isAfk: Boolean
)
