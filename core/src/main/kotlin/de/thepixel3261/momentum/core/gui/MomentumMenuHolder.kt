package de.thepixel3261.momentum.core.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class MomentumMenuHolder(private val page: Int) : InventoryHolder {
    private val tiers: HashMap<Int, Int> = hashMapOf()
    override fun getInventory(): Inventory {
        return null!!
    }

    fun getPage(): Int {
        return page
    }

    fun setTier(slot: Int, id: Int) {
        tiers[slot] = id
    }

    fun getTier(slot: Int): Int? {
        return tiers[slot]
    }
}