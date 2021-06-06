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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class StorageRepo (
    private val database: SQLite,
)  {

    private var groups = ConcurrentHashMap.newKeySet<Group>()
    private var senders = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build<Location, Group>())
    private var receivers = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build<Location, Group>())

    init { runBlocking { reload() } }

    suspend fun reload() {
        groups.clear()
        groups.addAll(database.getGroups())
        senders.clear()
        senders = groups.multimapByLocation { it.senders }
        receivers.clear()
        receivers = groups.multimapByLocation { it.receivers }
    }

    private fun Collection<Group>.multimapByLocation(selector: (Group) -> Set<Storage>): ImmutableMultimap<Location, Group> {
        val builder = ImmutableMultimap.builder<Location, Group>()
        forEach { group ->
            selector(group).mapNotNull { sender -> sender.toLocation().takeIf { it.world != null } }
                .forEach { builder.put(it, group) }
        }
        return builder.build()
    }

    fun getGroup(player: Player, name: String): Option<Group> = groups.firstOrNone { it.name.equals(name, ignoreCase = true) && it.owner == player.uniqueId }

    fun getGroups(): Set<Group> = groups

    fun getSenderStorages() = senders

    fun addGroup(player: Player, name: String) {
        groups.add(Group(name, player.uniqueId))
    }
}
