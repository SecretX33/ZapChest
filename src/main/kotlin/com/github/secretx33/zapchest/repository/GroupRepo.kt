package com.github.secretx33.zapchest.repository

import arrow.core.Option
import arrow.core.toOption
import com.github.secretx33.zapchest.database.SQLite
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.model.Storage
import com.github.secretx33.zapchest.model.toLocalPlayer
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Multimaps
import kotlinx.coroutines.runBlocking
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.BlockInventoryHolder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GroupRepo (
    private val database: SQLite,
)  {

    private val groups = ConcurrentHashMap<Pair<String, UUID>, Group>()
    private val senders = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build<Location, Pair<String, UUID>>())
    private val receivers = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build<Location, Pair<String, UUID>>())

    init { runBlocking { reload() } }

    suspend fun reload() {
        groups.clear()
        groups.putAll(database.getGroups())
        senders.clear()
        senders.putAll(groups.values.multimapByLocation { it.senders })
        receivers.clear()
        receivers.putAll(groups.values.multimapByLocation { it.receivers })
    }

    private fun Collection<Group>.multimapByLocation(selector: (Group) -> Set<Storage>): ImmutableMultimap<Location, Pair<String, UUID>> {
        val builder = ImmutableMultimap.builder<Location, Pair<String, UUID>>()
        forEach { group ->
            selector(group).mapNotNull { sender -> sender.toLocation().takeIf { it.world != null } }
                .forEach { builder.put(it, group.mapKey) }
        }
        return builder.build()
    }

    fun hasGroup(group: Group): Boolean = group in groups.values

    fun getGroup(player: Player, name: String): Option<Group> = groups[Pair(name.lowercase(Locale.US), player.uniqueId)].toOption()

    fun getGroupsThatPlayerOwns(player: Player): List<Group> = groups.values.filter { it.owner.uniqueId == player.uniqueId }.sortedBy { it.name }

    fun getGroupsThatPlayerBelongs(player: Player): List<Group> {
        val localPlayer = player.toLocalPlayer()
        return getGroupsThatPlayerOwns(player) + groups.values.filter { localPlayer in it.members }.sortedBy { it.name }
    }

    fun getSenderGroupsAt(location: Location): Set<Group> = senders[location].mapNotNullTo(HashSet()) { groups[it] }

    fun isStorage(holder: BlockInventoryHolder): Boolean = holder.inventory.location?.let { loc -> senders[loc].isNotEmpty() || receivers[loc].isNotEmpty() } ?: false

    fun hasSender(group: Group, holder: BlockInventoryHolder): Boolean {
        val location = holder.inventory.location ?: throw IllegalArgumentException("Holder $holder inventory doesn't have a location defined")
        return group.senders.any { location == it.toLocation() }
    }

    fun hasReceiver(group: Group, holder: BlockInventoryHolder): Boolean {
        val location = holder.inventory.location ?: throw IllegalArgumentException("Holder $holder inventory doesn't have a location defined")
        return group.receivers.any { location == it.toLocation() }
    }

    fun addSender(group: Group, block: Block) {
        require(block.state is BlockInventoryHolder) { "block needs to be BlockInventoryHolder, block = ${block.type} is not" }
        val newGroup = group.copy(senders = group.senders + Storage(block, emptySet()))
        groups[newGroup.mapKey] = newGroup
        database.updateGroupSenders(newGroup)
    }

    fun addReceiver(group: Group, block: Block) {
        require(block.state is BlockInventoryHolder) { "block needs to be BlockInventoryHolder, block = ${block.type} is not" }
        val newGroup = group.copy(receivers = group.receivers + Storage(block, emptySet()))
        groups[newGroup.mapKey] = newGroup
        database.updateGroupReceivers(newGroup)
    }

    fun unboundStorage(holder: BlockInventoryHolder) {
        val location = holder.inventory.location ?: return
        val holderGroups = (senders[location] + receivers[location]).mapNotNull { groups[it] }
        val newHolderGroups = holderGroups.map { group ->
            group.copy(senders = group.senders.filter { !it.isAt(location) }.toSet(),
                receivers = group.receivers.filter { !it.isAt(location) }.toSet())
        }
        groups.values.removeAll(holderGroups)
        groups.putAll(newHolderGroups.associateBy { it.mapKey })
        senders.removeAll(location)
        receivers.removeAll(location)
        database.updateGroupStorages(newHolderGroups)
    }

    fun createGroup(player: Player, groupName: String) {
        val group = Group(groupName.lowercase(Locale.US), player.toLocalPlayer())
        groups[group.mapKey] = group
        database.addStorageGroup(group)
    }

    fun addMemberToGroup(player: Player, group: Group) {
        val newGroup = group.copy(members = group.members + player.toLocalPlayer())
        groups[group.mapKey] = newGroup
        database.updateGroupMembers(group)
    }

    fun removeMemberOfGroup(player: Player, group: Group) {
        val newGroup = group.copy(members = group.members - player.toLocalPlayer())
        groups[group.mapKey] = newGroup
        database.updateGroupMembers(group)
    }

    fun addMaterialsToReceiver(group: Group, holder: BlockInventoryHolder, materials: Collection<Material>) {
        val location = holder.inventory.location ?: throw IllegalArgumentException("Holder $holder inventory doesn't have a location defined")
        val originalReceiver = group.receivers.first { it.isAt(location) }
        val modifiedReceiver = originalReceiver.copy(acceptMaterials = originalReceiver.acceptMaterials + materials)
        updateGroupReceiver(group, originalReceiver, modifiedReceiver)
    }

    fun removeMaterialsOfReceiver(group: Group, holder: BlockInventoryHolder, materials: Collection<Material>) {
        val location = holder.inventory.location ?: throw IllegalArgumentException("Holder $holder inventory doesn't have a location defined")
        val originalReceiver = group.receivers.first { it.isAt(location) }
        val modifiedReceiver = originalReceiver.copy(acceptMaterials = originalReceiver.acceptMaterials - materials)
        updateGroupReceiver(group, originalReceiver, modifiedReceiver)
    }

    private fun updateGroupReceiver(group: Group, originalReceiver: Storage, modifiedReceiver: Storage) {
        val newReceivers = group.receivers - originalReceiver + modifiedReceiver
        val newGroup = group.copy(receivers = newReceivers)
        database.updateGroupReceivers(newGroup)
    }
}
