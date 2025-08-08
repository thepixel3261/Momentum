package de.thepixel3261.momentum.session

import java.util.*

data class SessionData(
    val uuid: UUID,
    var joinTime: Long,
    var lastActivity: Long,
    var totalPlayMinutes: Int,
    var claimedTiers: MutableSet<Int>,
    var unlockedTiers: MutableSet<Int>,
    var isAfk: Boolean
)
