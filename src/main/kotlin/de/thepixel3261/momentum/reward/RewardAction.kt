package de.thepixel3261.momentum.reward

sealed class RewardAction {
    data class GiveMoney(val amount: Double) : RewardAction()
    data class GiveXP(val amount: Int) : RewardAction()
    data class RunCommand(val command: String) : RewardAction()
    data class PlaySound(val sound: String, val volume: Float, val pitch: Float) : RewardAction()
    data class ShowParticle(val particle: String, val count: Int) : RewardAction()
}
