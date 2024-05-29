package net.runelite.client.plugins.griffinplugins.griffintrainer.models.inventory

import net.runelite.client.plugins.griffinplugins.griffintrainer.models.DynamicItemSet
import net.runelite.client.plugins.microbot.util.inventory.Inventory

class InventoryRequirements {

    private val itemSets: MutableList<DynamicItemSet> = mutableListOf()

    fun getItemSets(): List<DynamicItemSet> {
        return itemSets.reversed()
    }

    fun addItemSet(itemSet: DynamicItemSet) {
        if (itemSet.getItems().isEmpty()) {
            return
        }
        itemSets.add(itemSet)
    }

    fun checkMeetsRequirements(): Boolean {
        Inventory.open()
        itemSets.forEach { dynamicItemSet: DynamicItemSet ->
            var meetsRequirement = false
            dynamicItemSet.getItems().forEach { itemAndQuantityPair: Triple<Int, Int, Boolean> ->
                if (Inventory.hasItem(itemAndQuantityPair.first)) {
                    if (Inventory.getItemAmount(itemAndQuantityPair.first) >= itemAndQuantityPair.second) {
                        meetsRequirement = true
                    }
                }
            }

            if (!meetsRequirement) {
                return false
            }
        }
        return true
    }
}