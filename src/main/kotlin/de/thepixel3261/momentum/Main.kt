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
import de.thepixel3261.momentum.util.PlaceholderUtil
import net.milkbowl.vault.economy.Economy
import org.bstats.bukkit.Metrics
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

    override fun onEnable() {
        // 1. Setup external dependencies
        if (!setupEconomy()) {
            logger.warning("Vault not found! Economy features will be disabled.")
        }

        val bStatsId = 26832
        Metrics(this, bStatsId)

        // 2. Instantiate managers
        rewardManager = RewardManager(economy)
        sessionManager = SessionManager(this)
        afkManager = AfkManager(this, sessionManager)
        configLoader = ConfigLoader(this, rewardManager)
        redisManager = RedisManager(this, configLoader)
        momentumMenu = MomentumMenu(this)

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
        getCommand("momentum")?.executor = MomentumCommand(this, rewardManager, momentumMenu)

        // 6. Start tasks
        afkManager.startAfkChecker()
        sessionManager.startRewardChecker()

        // 7. Register PlaceholderAPI expansion
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            PlaceholderUtil(this).register()
        }

        logger.info("Momentum plugin enabled.")
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
