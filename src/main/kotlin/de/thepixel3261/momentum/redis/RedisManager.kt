package de.thepixel3261.momentum.redis

import com.google.gson.Gson
import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.config.ConfigLoader
import de.thepixel3261.momentum.session.SessionData
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.util.*

class RedisManager(private val plugin: Main, private val configLoader: ConfigLoader) {
    var jedisPool: JedisPool? = null
    private val gson = Gson()

    fun connect() {
        if (!configLoader.redisEnabled) {
            plugin.logger.info("Redis is disabled in the config.")
            return
        }

        try {
            val poolConfig = JedisPoolConfig()
            jedisPool = if (configLoader.redisPassword.isNotBlank()) {
                JedisPool(poolConfig, configLoader.redisHost, configLoader.redisPort, 2000, configLoader.redisPassword)
            } else {
                JedisPool(poolConfig, configLoader.redisHost, configLoader.redisPort, 2000)
            }
            plugin.logger.info("Successfully connected to Redis.")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to connect to Redis: ${e.message}")
            jedisPool = null
        }
    }

    fun isLeaving(uuid: UUID): Boolean {
        if (jedisPool == null) return false
        try {
            jedisPool!!.resource.use { jedis ->
                return jedis.exists("momentum:leaving:$uuid")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Redis error: ${e.message}")
            return false
        }
    }

    fun setLeaving(data: SessionData) {
        if (jedisPool == null) return
        try {
            jedisPool!!.resource.use { jedis ->
                jedis.setex("momentum:leaving:${data.uuid}", 30, "true")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Redis error: ${e.message}")
        }
        saveSessionData(data)
    }

    fun clearLeaving(uuid: UUID) {
        if (jedisPool == null) return
        try {
            jedisPool!!.resource.use { jedis ->
                jedis.del("momentum:leaving:$uuid")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Redis error: ${e.message}")
        }
    }

    private fun saveSessionData(session: SessionData) {
        if (jedisPool == null) return
        try {
            val json = gson.toJson(session)
            jedisPool!!.resource.use { jedis ->
                jedis.setex("momentum:session:${session.uuid}", 30, json)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save session data: ${e.message}")
        }
    }

    fun getSessionData(uuid: UUID): SessionData? {
        if (jedisPool == null) return null
        return try {
            jedisPool!!.resource.use { jedis ->
                val json = jedis.get("momentum:session:$uuid") ?: return null
                gson.fromJson(json, SessionData::class.java)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load session data: ${e.message}")
            null
        }
    }

    fun clearSessionData(uuid: UUID) {
        if (jedisPool == null) return
        try {
            jedisPool!!.resource.use { jedis ->
                jedis.del("momentum:session:$uuid")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to clear session data: ${e.message}")
        }
    }

    fun disconnect() {
        jedisPool?.close()
    }
}
