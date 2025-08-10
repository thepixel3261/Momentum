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

package de.thepixel3261.momentum

import de.thepixel3261.momentum.afk.AfkListener
import de.thepixel3261.momentum.afk.AfkManager
import de.thepixel3261.momentum.command.MomentumCommand
import de.thepixel3261.momentum.config.ConfigLoader
import de.thepixel3261.momentum.gui.GuiListener
import de.thepixel3261.momentum.gui.MomentumMenu
import de.thepixel3261.momentum.redis.RedisManager
import de.thepixel3261.momentum.reward.RewardManager
import de.thepixel3261.momentum.session.SessionListener
import de.thepixel3261.momentum.session.SessionManager
import de.thepixel3261.momentum.util.bStats.BStatsUtil
import de.thepixel3261.momentum.util.PlaceholderUtil
import de.thepixel3261.momentum.util.VersionUtil
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin


class Main : JavaPlugin() {

    private var economy: Economy? = null

    lateinit var configLoader: ConfigLoader
    lateinit var rewardManager: RewardManager
    lateinit var sessionManager: SessionManager
    lateinit var afkManager: AfkManager
    lateinit var redisManager: RedisManager
    lateinit var momentumMenu: MomentumMenu
    lateinit var versionUtil: VersionUtil

    override fun onEnable() {
        // 1. Setup external dependencies
        if (!setupEconomy()) {
            logger.warning("Vault not found! Economy features will be disabled.")
        }

        // Set up bStats
        BStatsUtil(this)

        // 2. Instantiate managers
        rewardManager = RewardManager(economy)
        sessionManager = SessionManager(this)
        afkManager = AfkManager(this, sessionManager)
        configLoader = ConfigLoader(this, rewardManager)
        redisManager = RedisManager(this, configLoader)
        momentumMenu = MomentumMenu(this)
        versionUtil = VersionUtil(this)

        // 3. Inject dependencies
        rewardManager.sessionManager = sessionManager
        sessionManager.rewardManager = rewardManager

        // 4. Load configs and connect to services
        configLoader.load()
        redisManager.connect()

        // 5. Register listeners and commands
        server.pluginManager.registerEvents(SessionListener(sessionManager, redisManager), this)
        server.pluginManager.registerEvents(AfkListener(afkManager), this)
        server.pluginManager.registerEvents(GuiListener(this), this)
        server.pluginManager.registerEvents(VersionUtil(this), this)
        getCommand("momentum")?.executor = MomentumCommand(this, rewardManager, momentumMenu)

        // 6. Start tasks
        afkManager.startAfkChecker()
        sessionManager.startRewardChecker()

        // 7. Register PlaceholderAPI expansion
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            PlaceholderUtil(this).register()
        }

        logger.info("Momentum plugin enabled.")

        server.consoleSender.sendMessage("---§aMomentum§f---")
        server.consoleSender.sendMessage("Version: §3${description.version}")
        server.consoleSender.sendMessage("Author: §6thepixel3261")
        if (versionUtil.getUpdateMessage() != null) {
            server.consoleSender.sendMessage("${versionUtil.getUpdateMessage()}")
        }
    }

    override fun onDisable() {
        redisManager.disconnect()
        logger.info("Momentum plugin disabled.")
    }

    private fun setupEconomy(): Boolean {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false
        }

        val rsp = server.servicesManager.getRegistration(Economy::class.java) ?: return false
        economy = rsp.provider
        return economy != null
    }
}
