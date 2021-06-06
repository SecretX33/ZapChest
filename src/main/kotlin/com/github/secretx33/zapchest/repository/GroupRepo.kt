package com.github.secretx33.zapchest.repository

import arrow.core.Option
import arrow.core.firstOrNone
import com.github.secretx33.zapchest.database.SQLite
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.model.Storage
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Multimaps
import kotlinx.coroutines.runBlocking
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

class GroupRepo (
    private val database: SQLite,
)  {

    private val groups = ConcurrentHashMap.newKeySet<Group>()
    private val senders = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build<Location, Group>())
    private val receivers = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build<Location, Group>())

    init { runBlocking { reload() } }

    suspend fun reload() {
        groups.clear()
        groups.addAll(database.getGroups())
        senders.clear()
        senders.putAll(groups.multimapByLocation { it.senders })
        receivers.clear()
        receivers.putAll(groups.multimapByLocation { it.receivers })
    }

    private fun Collection<Group>.multimapByLocation(selector: (Group) -> Set<Storage>): ImmutableMultimap<Location, Group> {
        val builder = ImmutableMultimap.builder<Location, Group>()
        forEach { group ->
            selector(group).mapNotNull { sender -> sender.toLocation().takeIf { it.world != null } }
                .forEach { builder.put(it, group) }
        }
        return builder.build()
    }

    fun hasGroup(group: Group) = group in groups

    fun getGroups(player: Player): Collection<Group> = groups.filter { it.owner == player.uniqueId }

    fun getGroup(player: Player, name: String): Option<Group> = groups.firstOrNone { it.name.equals(name, ignoreCase = true) && it.owner == player.uniqueId }

    fun getGroups(): Set<Group> = groups

    fun getSenderStorages() = senders

    fun createGroup(player: Player, groupName: String) {
        groups.add(Group(groupName, player.uniqueId))
    }

    fun addMemberToGroup(player: Player, group: Group) {
        groups.remove(group)
        groups.add(group.copy(members = group.members + player.uniqueId))
    }

}