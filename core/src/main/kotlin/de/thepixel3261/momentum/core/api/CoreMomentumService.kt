package de.thepixel3261.momentum.core.api

import de.thepixel3261.momentum.api.*
import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.session.SessionData
import de.thepixel3261.momentum.core.session.SessionManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CoreMomentumService(private val plugin: Main, private val sessionManager: SessionManager) : MomentumService {
    private val sessionSvc = CoreSessionService(sessionManager)
    private val registry = CoreRewardActionRegistry(sessionSvc)

    override fun sessions(): SessionService = sessionSvc
    override fun rewards(): RewardActionRegistry = registry
}

private class CoreSessionService(private val sessionManager: SessionManager) : SessionService {
    override fun get(uuid: UUID): SessionSnapshot? = sessionManager.getSessionByUUID(uuid)?.toSnapshot()

    override fun modify(uuid: UUID, modify: (MutableSession) -> Unit) {
        val player: Player? = Bukkit.getPlayer(uuid)
        val data = player?.let { sessionManager.getSession(it) } ?: player?.let { sessionManager.startSession(it) }
        if (data != null) {
            val adapter = DataMutableSessionAdapter(data)
            modify(adapter)
        }
    }
}

private class DataMutableSessionAdapter(private val data: SessionData) : MutableSession {
    override var totalPlayMinutes: Int
        get() = data.totalPlayMinutes
        set(value) { data.totalPlayMinutes = value }
    override var claimedTiers: MutableSet<Int>
        get() = data.claimedTiers
        set(value) { data.claimedTiers = value }
    override var unlockedTiers: MutableSet<Int>
        get() = data.unlockedTiers
        set(value) { data.unlockedTiers = value }
    override var isAfk: Boolean
        get() = data.isAfk
        set(value) { data.isAfk = value }
    override var multiplier: Double
        get() = data.multiplier
        set(value) { data.multiplier = value }
    override var lastRecycle: Int
        get() = data.lastRecycle
        set(value) { data.lastRecycle = value }
    override var recycles: Int
        get() = data.recycles
        set(value) { data.recycles = value }
}

private fun SessionData.toSnapshot(): SessionSnapshot = SessionSnapshot(
    uuid = uuid,
    joinTime = joinTime,
    lastActivity = lastActivity,
    totalPlayMinutes = totalPlayMinutes,
    claimedTiers = claimedTiers.toSet(),
    unlockedTiers = unlockedTiers.toSet(),
    isAfk = isAfk,
    multiplier = multiplier,
    lastRecycle = lastRecycle,
    recycles = recycles,
)

class CoreRewardActionRegistry(private val sessions: SessionService) : RewardActionRegistry {
    private val executors = ConcurrentHashMap<String, RewardActionExecutor>()

    override fun register(id: String, executor: RewardActionExecutor, override: Boolean) {
        val key = id.lowercase()
        if (!override && executors.containsKey(key)) return
        executors[key] = executor
    }

    override fun unregister(id: String) {
        executors.remove(id.lowercase())
    }

    override fun executorFor(id: String): RewardActionExecutor? = executors[id.lowercase()]

    fun contextFor(uuid: UUID, params: Map<String, Any?>, visible: Boolean, lore: List<String>?): RewardActionContext? {
        val snap = sessions.get(uuid) ?: return null
        return RewardActionContext(
            uuid = uuid,
            session = snap,
            params = params,
            visible = visible,
            lore = lore,
            multiplier = snap.multiplier,
        )
    }
}
