package de.thepixel3261.momentum.reward

data class RewardTier(
    val id: Int,
    val unlockAfterMinutes: Int,
    val actions: List<RewardAction>
)
