package com.github.secretx33.zapchest.model

import com.github.secretx33.zapchest.util.extension.compareBlockLocation
import com.github.secretx33.zapchest.util.extension.formattedLocation
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.DoubleChest
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.inventory.Inventory
import java.util.EnumSet
import java.util.Objects
import java.util.UUID

class Storage(block: Block, acceptMaterials: Set<Material>) {

    val worldUuid: UUID = block.world.uid
    val x: Int
    val y: Int
    val z: Int
    val acceptMaterials: Set<Material> = if(acceptMaterials.isEmpty()) emptySet() else acceptMaterials as? EnumSet<Material> ?: EnumSet.copyOf(acceptMaterials)

    init {
        require(block.state is BlockInventoryHolder) { "Block at position ${block.formattedLocation()} is not a BlockInventoryHolder" }
        val location = (block.state as BlockInventoryHolder).inventory.location ?: throw IllegalStateException("BlockInventoryHolder at position ${block.formattedLocation()} have no location, that shouldn't be possible")
        x = location.blockX
        y = location.blockY
        z = location.blockZ
    }

    val block: Block
        get() {
            val world = Bukkit.getWorld(worldUuid) ?: throw IllegalStateException("World $worldUuid is unloaded")
            return world.getBlockAt(x, y, z).takeIf { it.state is BlockInventoryHolder } ?: throw IllegalStateException("Block at position ${formattedLocation()} is not a BlockInventoryHolder!")
        }

    val contents: Inventory
        get() = (block.state as? BlockInventoryHolder)?.inventory ?: throw IllegalStateException("Block at position ${formattedLocation()} is not a BlockInventoryHolder!")

    fun isAt(location: Location): Boolean {
        val world = Bukkit.getWorld(worldUuid) ?: return false
        val state = world.getBlockAt(x, y, z).state

        if(state is DoubleChest) {
            val left = state.leftSide?.inventory?.location
            val right = state.rightSide?.inventory?.location

            return left?.compareBlockLocation(location) == true || right?.compareBlockLocation(location) == true
        }
        return worldUuid == location.world?.uid && x == location.blockX && y == location.blockY && z == location.blockZ
    }

    fun toLocation() = Location(Bukkit.getWorld(worldUuid), x.toDouble(), y.toDouble(), z.toDouble())

    private fun formattedLocation(): String {
        val world = Bukkit.getWorld(worldUuid)
        return "(world = ${world?.name ?: "null"} ($worldUuid), x = $x, y = $y, z = $z)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Storage
        if (worldUuid != other.worldUuid) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int = Objects.hash(worldUuid, x, y, z)

    override fun toString() = "Storage(worldUuid=$worldUuid, x=$x, y=$y, z=$z)"

    fun copy(block: Block = this.block, acceptMaterials: Set<Material> = this.acceptMaterials) = Storage(block, acceptMaterials)
}

//fun Location.toStorage(): Storage {
//    val world = world ?: throw IllegalStateException("Location world is null, Storage cannot hold null worlds")
//    val block = world.getBlockAt(blockX, blockY, blockZ).takeIf { it.state is BlockInventoryHolder } ?: throw IllegalStateException("Block at position ${formattedString()} is not an BlockInventoryHolder, which means it was NOT removed when it was broken, report this")
//    return Storage(block)
//}
