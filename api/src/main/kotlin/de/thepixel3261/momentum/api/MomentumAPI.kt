package de.thepixel3261.momentum.api

import java.util.*

/**
 * Root service exposed by Momentum for other plugins.
 * Acquire via Bukkit ServicesManager or the static accessor [MomentumAPI.get()].
 */
interface MomentumService {
    fun sessions(): SessionService
    fun rewards(): RewardActionRegistry
}

/** Session read/write access for other plugins. */
interface SessionService {
    fun get(uuid: UUID): SessionSnapshot?

    /** Modify a player's session atomically; creates a session if missing. */
    fun modify(uuid: UUID, modify: (MutableSession) -> Unit)
}

/** Immutable view of a player's session. */
data class SessionSnapshot(
    val uuid: UUID,
    val joinTime: Long,
    val lastActivity: Long,
    val totalPlayMinutes: Int,
    val claimedTiers: Set<Int>,
    val unlockedTiers: Set<Int>,
    val isAfk: Boolean,
    val multiplier: Double,
    val lastRecycle: Int,
    val recycles: Int,
)

/** Mutable façade to edit selected fields of a session safely. */
interface MutableSession {
    var totalPlayMinutes: Int
    var claimedTiers: MutableSet<Int>
    var unlockedTiers: MutableSet<Int>
    var isAfk: Boolean
    var multiplier: Double
    var lastRecycle: Int
    var recycles: Int
}

/** Registry for reward action executors, addressable by config "type" id. */
interface RewardActionRegistry {
    /** Register a new custom action id. Fails if id already exists unless [override] is true. */
    fun register(id: String, executor: RewardActionExecutor, override: Boolean = false)

    /** Remove a previously registered action id. */
    fun unregister(id: String)

    /** Fetch the executor for an id if present. */
    fun executorFor(id: String): RewardActionExecutor?
}

/** Executes an action for a player; params come from rewards.yml action map. */
fun interface RewardActionExecutor {
    fun execute(ctx: RewardActionContext)
}

/** Context provided to executors. */
data class RewardActionContext(
    val uuid: UUID,
    val session: SessionSnapshot,
    /** Raw parameters from config for this action (e.g., amount, command, etc.). */
    val params: Map<String, Any?>,
    /** Whether the action is visible in UI and optional lore defined in config. */
    val visible: Boolean,
    val lore: List<String>?,
    /** Multiplier currently applied to the player. */
    val multiplier: Double,
)

/** Static accessor utility to fetch the service from Bukkit's ServicesManager. */
object MomentumAPI {
    @Volatile
    private var instance: MomentumService? = null

    fun get(): MomentumService? = instance

    // Core sets/unsets this via reflection-safe calls; methods kept internal to avoid ABI leaks.
    fun internalSet(service: MomentumService?) {
        instance = service
    }
}
