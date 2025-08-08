package de.thepixel3261.momentum.gui

import de.thepixel3261.momentum.Main
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class GuiListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.title != plugin.configLoader.guiTitle) return

        event.isCancelled = true

        val player = event.whoClicked as? org.bukkit.entity.Player ?: return

        // Logic to handle clicks will go here
        val clickedItem = event.currentItem ?: return

        // Claim All button
        if (clickedItem.itemMeta?.displayName == ChatColor.translateAlternateColorCodes('&', plugin.configLoader.claimAllItemName)) {
            plugin.rewardManager.claimAllTiers(player)
            player.closeInventory()
            return
        }

        // Tier items
        val tierIdString = clickedItem.itemMeta?.displayName?.substringAfter("Tier ")?.substringBefore(" ")
        val tierId = tierIdString?.toIntOrNull() ?: return

        plugin.rewardManager.claimTier(player, tierId)
        plugin.momentumMenu.open(player) // Refresh GUI
    }
}
