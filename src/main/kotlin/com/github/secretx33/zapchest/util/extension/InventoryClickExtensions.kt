package com.github.secretx33.zapchest.util.extension

import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType

// PICK (top inventory perspective)

val InventoryClickEvent.isItemPick: Boolean get() {
    // shift click on item in gui inventory
    if(action == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory?.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
        return true

    // normal click on an item in gui inventory
    if(action.isPickUp() && clickedInventory?.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
        return true

    return false
}

private fun InventoryAction.isPickUp() = this == InventoryAction.PICKUP_ALL || this == InventoryAction.PICKUP_HALF || this == InventoryAction.PICKUP_SOME || this == InventoryAction.PICKUP_ONE

// PUT (top inventory perspective)

val InventoryClickEvent.isItemPlace: Boolean get() {
    // shift click on item in player inventory
    if(action == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory?.type == InventoryType.PLAYER && clickedInventory?.type != inventory.type)
        return true

    // normal click on gui empty slot with item on cursor
    if(action.isPlace() && clickedInventory?.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
        return true

    return false
}

private fun InventoryAction.isPlace() = this == InventoryAction.PLACE_ALL || this == InventoryAction.PLACE_SOME || this == InventoryAction.PLACE_ONE

// SWAP (top inventory perspective)

val InventoryClickEvent.isItemSwap: Boolean get() {
    // normal click on gui item slot with item on cursor (to swap)
    if(action == InventoryAction.SWAP_WITH_CURSOR && clickedInventory?.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
        return true

    return false
}

// DRAG (on top inventory)

fun InventoryDragEvent.isDragOnTopInventory(): Boolean {
    val topInvSize = view.topInventory.size.takeIf { view.topInventory != view.bottomInventory }
        ?: return false
    // player has dragged items inside top inventory
    return rawSlots.any { it < topInvSize }
}

