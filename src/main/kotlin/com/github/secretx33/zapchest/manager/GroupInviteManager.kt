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
            ?: return GroupJoinResponse.NON_EXISTENT

        invites.invalidate(player.uniqueId)
        if(!groupRepo.hasGroup(group)) return GroupJoinResponse.GROUP_REMOVED

        groupRepo.addMemberToGroup(player, group)
        return GroupJoinResponse.JOINED
    }

    private companion object {
        const val GRACE_TIME = 20000L
    }
}
