package com.github.secretx33.zapchest.manager

import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.model.GroupJoinResponse
import com.github.secretx33.zapchest.repository.GroupRepo
import com.google.common.cache.CacheBuilder
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.TimeUnit

class GroupInviteManager (
    private val groupRepo: GroupRepo,
) {

    private val invites = CacheBuilder.newBuilder().expireAfterWrite(GRACE_TIME, TimeUnit.MILLISECONDS).build<Pair<UUID, String>, Pair<Long, Group>>()

    fun createInvite(player: Player, group: Group) {
        invites.put(Pair(player.uniqueId, group.name), Pair(System.currentTimeMillis() + GRACE_TIME, group))
    }

    fun acceptInvite(player: Player, groupName: String): GroupJoinResponse {
        val group = invites.getIfPresent(Pair(player.uniqueId, groupName))
            ?.takeIf { it.first > System.currentTimeMillis() }?.second
            ?: return GroupJoinResponse(GroupJoinResponse.Reason.NON_EXISTENT, null)

        invites.invalidate(player.uniqueId)
        if(!groupRepo.hasGroup(group)) return GroupJoinResponse(GroupJoinResponse.Reason.GROUP_REMOVED, group)

        groupRepo.addMemberToGroup(player, group)
        return GroupJoinResponse(GroupJoinResponse.Reason.JOINED, group)
    }

    fun getAllInvites(player: Player): List<String> = invites.asMap().filter { it.key.first == player.uniqueId && it.value.first > System.currentTimeMillis() }.map { it.key.second }

    private companion object {
        const val GRACE_TIME = 20000L
    }
}
