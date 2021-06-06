package com.github.secretx33.zapchest.util.extension

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin

fun runSync(plugin: Plugin, delay: Long = 0L, runnable: Runnable) {
    if(delay < 0) return
    if(delay == 0L) Bukkit.getScheduler().runTask(plugin, runnable)
    else Bukkit.getScheduler().runTaskLater(plugin, runnable, delay / 50L)
}

fun Location.compareBlockLocation(other: Location) = world?.uid == other.world?.uid && blockX == other.blockX  && blockY == other.blockY && blockZ == other.blockZ

fun Location.formattedString(): String = "World: ${world?.name ?: "null"} (${world?.uid}), ${x.toLong()}, ${y.toLong()}, ${z.toLong()}"
