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

// CREDITS: https://youtu.be/JwSqUnncVk8?si=4XEf3d6GE6oAsW7d

package de.thepixel3261.momentum.core.util

import de.thepixel3261.momentum.core.Main
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

object Scheduler {
    var isFolia: Boolean = false
        private set

    private lateinit var plugin: Main

    fun init(plugin: Main) {
        this.plugin = plugin
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            isFolia = true
        } catch (e: ClassNotFoundException) {
            isFolia = false
        }
    }

    fun run(runnable: Runnable?) {
        if (isFolia) Bukkit.getGlobalRegionScheduler()
            .execute(plugin, runnable!!)
        else Bukkit.getScheduler().runTask(plugin, runnable!!)
    }

    fun runLater(runnable: Runnable, delayTicks: Long): Task {
        return if (isFolia) Task(
            Bukkit.getGlobalRegionScheduler()
                .runDelayed(
                    plugin,
                    { t: ScheduledTask? -> runnable.run() }, delayTicks
                )
        )
        else Task(Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks))
    }

    fun runTimer(runnable: Runnable, delayTicks: Long, periodTicks: Long): Task {
        return if (isFolia) Task(
            Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(
                    plugin,
                    { t: ScheduledTask? -> runnable.run() }, if (delayTicks < 1) 1 else delayTicks, periodTicks
                )
        )
        else Task(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks))
    }

    class Task {
        private var foliaTask: Any? = null
        private var bukkitTask: BukkitTask? = null

        internal constructor(foliaTask: Any?) {
            this.foliaTask = foliaTask
        }

        internal constructor(bukkitTask: BukkitTask?) {
            this.bukkitTask = bukkitTask
        }

        fun cancel() {
            if (foliaTask != null) (foliaTask as ScheduledTask).cancel()
            else bukkitTask!!.cancel()
        }
    }
}