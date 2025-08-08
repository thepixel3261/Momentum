package de.thepixel3261.momentum.util

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

object ItemUtil {
    fun create(material: Material, name: String, lore: List<String> = emptyList(), glowing: Boolean = false): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        meta.displayName = ChatColor.translateAlternateColorCodes('&', name)
        meta.lore = lore.map { ChatColor.translateAlternateColorCodes('&', it) }

        if (glowing) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }

        item.itemMeta = meta
        return item
    }
}
