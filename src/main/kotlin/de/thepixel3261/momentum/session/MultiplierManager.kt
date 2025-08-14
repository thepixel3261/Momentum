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


package de.thepixel3261.momentum.session

import org.bukkit.Bukkit

object MultiplierManager {
    var recycleMultiplier: Double = 1.0
    fun SessionData.addMultiplier(value: Double) {
        this.multiplier *= value
    }

    fun SessionData.setInitialMultiplier() {
        val player = Bukkit.getPlayer(this.uuid)

        var highestMultiplier: Double = this.multiplier
        player.effectivePermissions.forEach { permission ->
            if (permission.permission.startsWith("momentum.multiplier", ignoreCase = true) && permission.value) {
                val value: Double = permission.permission.substringAfterLast(".").replace("_", ".").toDoubleOrNull() ?: 1.0
                if (value > highestMultiplier) highestMultiplier = value
            }
        }
        this.multiplier = highestMultiplier
    }

    fun SessionData.recycle(): SessionData {
        this.lastRecycle = this.totalPlayMinutes
        this.addMultiplier(recycleMultiplier)
        this.claimedTiers = mutableSetOf()
        this.unlockedTiers = mutableSetOf()
        return this
    }

    fun setRecycleMultiplier(value: Double) {
        recycleMultiplier = value
    }
}