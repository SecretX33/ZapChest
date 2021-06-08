package com.github.secretx33.zapchest.model

data class GroupJoinResponse(
    val reason: Reason,
    val group: Group?,
) {
    enum class Reason {
        GROUP_REMOVED,
        JOINED,
        NON_EXISTENT,
    }
}


