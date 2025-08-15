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

package de.thepixel3261.momentum

import de.thepixel3261.momentum.afk.AfkListener
import de.thepixel3261.momentum.afk.AfkManager
import de.thepixel3261.momentum.command.MomentumCommand
import de.thepixel3261.momentum.config.ConfigLoader
import de.thepixel3261.momentum.gui.GuiListener
import de.thepixel3261.momentum.gui.MomentumMenu
import de.thepixel3261.momentum.lang.LanguageManager
import de.thepixel3261.momentum.lang.LanguageParser.translate
import de.thepixel3261.momentum.redis.RedisManager
import de.thepixel3261.momentum.reward.RewardManager
import de.thepixel3261.momentum.session.SessionListener
import de.thepixel3261.momentum.session.SessionManager
import de.thepixel3261.momentum.util.BstatsUtil
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
    lateinit var languageManager: LanguageManager
    var placeholderAPIv: String? = null
    var vaultv: String? = null
    var loadTime: Long = 0

    override fun onEnable() {
        loadTime = System.currentTimeMillis()
        // 1. Setup external dependencies
        if (!setupEconomy()) {
            logger.warning("Vault not found! Economy features will be disabled.")
        }

        // 2. Instantiate managers
        rewardManager = RewardManager(economy, this)
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
        getCommand("momentum")?.executor = MomentumCommand(this, momentumMenu)

        // 6. Start tasks
        afkManager.startAfkChecker()
        sessionManager.startRewardChecker()

        // 7. Register PlaceholderAPI expansion
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            PlaceholderUtil(this).register()
            placeholderAPIv = server.pluginManager.getPlugin("PlaceholderAPI").description.version
        }

        // 8. Load languages (again because of ConfigLoader)
        languageManager = LanguageManager(this)
        BstatsUtil(this)

        logger.info(startUpLog().joinToString("\n"))
        if (versionUtil.getUpdateMessage() != null) {
            Bukkit.getConsoleSender().sendMessage(versionUtil.getUpdateMessage())
        }

        Bukkit.getConsoleSender().sendMessage("%lang_test%".translate())
    }

    override fun onDisable() {
        redisManager.disconnect()
        logger.info("Momentum plugin disabled.")
    }

    private fun setupEconomy(): Boolean {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false
        }

        vaultv = server.pluginManager.getPlugin("Vault").description.version

        val rsp = server.servicesManager.getRegistration(Economy::class.java) ?: return false
        economy = rsp.provider
        return economy != null
    }

    fun startUpLog(): List<String> {
        val boxWidth = 40
        val title = "Momentum v${description.version}".padEnd(boxWidth - 4, ' ').let {
            if (it.length > boxWidth - 4) it.take(boxWidth - 2) else it
        }

        val redisInfo = "Redis ${if (configLoader.redisSsl) "(SSL Enabled)" else ""}"
        val crossServerSync = if (configLoader.redisEnabled) "ENABLED" else "DISABLED"
        val rewardsCount = configLoader.tierCount
        val placeholdersCount = if (server.pluginManager.getPlugin("PlaceholderAPI") != null) 2 else 0
        val loadTime = System.currentTimeMillis() - loadTime
        val lang = configLoader.lang

        val messages = mutableListOf(
            "",
            "┌${"─".repeat(boxWidth - 2)}┐",
            "│ ${title} │",
            "├${"─".repeat(boxWidth - 2)}┤",
            "│ Author: ${String.format("%-${boxWidth - 12}s", "thepixel3261")} │",
            "│ Language: ${String.format("%-${boxWidth - 14}s", lang)} │",
            "│ Storage: ${String.format("%-${boxWidth - 13}s", redisInfo)} │",
            "│ Cross-Server Sync: ${String.format("%-${boxWidth - 23}s", crossServerSync)} │",
            "│ Rewards Loaded: ${String.format("%-${boxWidth - 20}s", rewardsCount)} │",
            "│ PlaceholderAPI: ${String.format("%-${boxWidth - 20}s", "$placeholdersCount placeholders")} │",
            "│ Load Time: ${String.format("%-${boxWidth - 15}s", "$loadTime ms")} │",
            "└${"─".repeat(boxWidth - 2)}┘"
        )

        return messages
    }
}
