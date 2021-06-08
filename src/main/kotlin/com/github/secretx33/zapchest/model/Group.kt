package com.github.secretx33.zapchest.model

import org.bukkit.Material
import java.util.Locale
import java.util.Objects

data class Group(
    val name: String,
    val owner: LocalPlayer,
    val members: Set<LocalPlayer> = emptySet(),
    val senders: Set<Storage> = emptySet(),
    val receivers: Set<Storage> = emptySet(),
) {

    init { require(owner !in members) { "group owner cannot be inside members list" } }

    val mapKey = name to owner.uniqueId

    fun receiversFor(material: Material): Set<Storage> = receivers.filterTo(HashSet()) { material in it.acceptMaterials }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group
        return name.equals(other.name, ignoreCase = true) && owner == other.owner
    }

    override fun hashCode(): Int = Objects.hash(name.lowercase(Locale.US), owner)
}
