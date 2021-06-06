package com.github.secretx33.zapchest.model

import org.bukkit.Material
import java.util.Locale
import java.util.Objects
import java.util.UUID

data class Group(
    val name: String,
    val owner: UUID,
    val members: Set<UUID> = emptySet(),
    val senders: Set<Storage> = emptySet(),
    val receivers: Set<Storage> = emptySet(),
) {

    fun receiversFor(material: Material): Set<Storage> = receivers.filterTo(HashSet()) { material in it.acceptMaterials }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group
        return name.equals(other.name, ignoreCase = true) && owner == other.owner
    }

    override fun hashCode(): Int = Objects.hash(name.lowercase(Locale.US), owner)
}
