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

package de.thepixel3261.momentum.util

import com.google.gson.Gson
import de.thepixel3261.momentum.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class VersionUtil (val plugin: Main) : Listener {
    private val version = plugin.description.version.removeSurrounding("'").removeSuffix("-SNAPSHOT")
    private val gitHubReleasesURL = "https://api.github.com/repos/thepixel3261/Momentum/releases/latest"
    private val notifiedAdmins = mutableSetOf<UUID>()
    private var latestVersion: String? = null
    private var updateMessage: String? = null

    init {
        checkForUpdates()
    }

    private fun checkForUpdates() {
        if (latestVersion != null) return
        if (plugin.description.version.endsWith("-SNAPSHOT")) return

        try {
            val url = URL(gitHubReleasesURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                
            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                val releaseInfo = Gson().fromJson(response, Map::class.java)
                val latestTag = releaseInfo["tag_name"]?.toString()?.removePrefix("v") ?: return

                latestVersion = latestTag
                updateMessage = getUpdateMessage()
            } else{
                latestVersion = "Could not check for updates"
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to check for updates: ${e.message}")
            latestVersion = "Could not check for updates"
        }
    }

    private fun notifyPlayer(player: Player) {
        if (!player.isOp || notifiedAdmins.contains(player.uniqueId) || updateMessage == null) return

        // Get the Adventure audience for the player
        val audience = player as net.kyori.adventure.audience.Audience

        // Send the update message
        audience.sendMessage(Component.text("\n$updateMessage"))

        // Create and send Modrinth link
        val modrinthLink = Component.text()
            .content("Click here to download latest version (Modrinth)\n")
            .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
            .clickEvent(ClickEvent.openUrl(
                "https://modrinth.com/plugin/momentum-rewards/version/${latestVersion!!}"
            ))
            .hoverEvent(HoverEvent.showText(
                Component.text("Open Modrinth")
            ))
            .build()

        // Create and send GitHub link
        val githubLink = Component.text()
            .content("Click here to download the latest version (GitHub)")
            .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
            .clickEvent(
                ClickEvent.openUrl(
                "https://github.com/thepixel3261/Momentum/releases/latest"
            ))
            .hoverEvent(
                HoverEvent.showText(
                Component.text("Open GitHub")
            ))
            .build()

        // Send both links
        audience.sendMessage(modrinthLink)
        audience.sendMessage(githubLink)

        notifiedAdmins.add(player.uniqueId)
    }


    fun getUpdateMessage(): String? {
        val newVersion = latestVersion ?: return null
        if (!isNewerVersion(newVersion)) return null
        
        val currentParts = version.split('.').map { it.toInt() }
        val newParts = newVersion.split('.').map { it.toInt() }

        return when {
            newParts[0] > currentParts[0] -> { // Major update
                """
                [§aMomentum§f]
                === §cIMPORTANT UPDATE§f ===
                A new major version (§3$newVersion§f) is available!
                This update contains breaking changes. Please read the changelog before updating.
                """.trimIndent()
            }
            newParts[1] > currentParts[1] -> { // Minor update
                "[§aMomentum§f]\nA new version (§3$newVersion§f) is available! New features and improvements — update recommended."
            }
            else -> { // Patch update
                "[§aMomentum§f]\nA new patch (§3$newVersion§f) is available! Fixes bugs and improves stability."
            }
        }
    }
    
    private fun isNewerVersion(newVersion: String): Boolean {
        if (newVersion == version) return false
        
        val currentParts = version.split('.').map { it.toInt() }
        val newParts = newVersion.split('.').map { it.toInt() }
        
        return (0..2).any { i ->
            newParts.getOrNull(i)?.let { it > currentParts.getOrElse(i) { 0 } } == true
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        notifyPlayer(event.player)
    }
}
