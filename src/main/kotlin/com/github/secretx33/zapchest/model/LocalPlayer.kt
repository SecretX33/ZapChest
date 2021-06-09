package com.github.secretx33.zapchest.model

import arrow.core.Option
import arrow.core.toOption
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID

data class LocalPlayer(
    val name: String,
    val uniqueId: UUID,
) {
    fun toBukkitPlayer(): Option<Player> = Bukkit.getPlayer(uniqueId).toOption()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalPlayer
        return uniqueId == other.uniqueId
    }

    override fun hashCode(): Int = uniqueId.hashCode()
}

fun Player.toLocalPlayer(): LocalPlayer = LocalPlayer(name.lowercase(Locale.US), uniqueId)
