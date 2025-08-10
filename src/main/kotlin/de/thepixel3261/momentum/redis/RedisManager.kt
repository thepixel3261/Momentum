/*
 * [Momentum] Rewards and More.
 * Copyright (C) 2025 thepixel3261
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Additional terms (Section 7, AGPL‑3.0-or‑later):
 * If you modify and run this plugin on a publicly accessible Minecraft server,
 * you must publish the complete modified source via a public repository;
 * providing source “on request” does NOT satisfy this requirement.
 *
 * See LICENSE (bottom) for full terms.
 */


package de.thepixel3261.momentum.redis

import com.google.gson.Gson
import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.config.ConfigLoader
import de.thepixel3261.momentum.session.SessionData
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.exceptions.JedisConnectionException
import java.util.*
import java.util.logging.Level

class RedisManager(private val plugin: Main, private val configLoader: ConfigLoader) {
    var jedisPool: JedisPool? = null
    private val gson = Gson()
    private val maxRetryAttempts = 3
    private val retryDelayMs = 1000

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
        return withRetry("isLeaving") { jedis ->
            jedis.exists("momentum:leaving:$uuid")
        } ?: false
    }

    fun setLeaving(data: SessionData) {
        withRetry("setLeaving") { jedis ->
            jedis.setex("momentum:leaving:${data.uuid}", 30, "true")
        }
        saveSessionData(data)
    }

    fun clearLeaving(uuid: UUID) {
        withRetry("clearLeaving") { jedis ->
            jedis.del("momentum:leaving:$uuid")
        }
    }

    private fun saveSessionData(session: SessionData) {
        withRetry("saveSessionData") { jedis ->
            val json = gson.toJson(session)
            jedis.setex("momentum:session:${session.uuid}", 30, json)
        }
    }

    fun getSessionData(uuid: UUID): SessionData? {
        return withRetry("getSessionData") { jedis ->
            val json = jedis.get("momentum:session:$uuid")
            gson.fromJson(json, SessionData::class.java)
        }
    }

    fun clearSessionData(uuid: UUID) {
        withRetry("clearSessionData") { jedis ->
            jedis.del("momentum:session:$uuid")
        }
    }

    fun disconnect() {
        withRetry("disconnect", Jedis::disconnect) ?: jedisPool?.close()
    }

    private fun <T> withRetry(operation: String, block: (Jedis) -> T): T? {

        var lastException: Exception? = null
        var result: T? = null

        for (attempt in 1..maxRetryAttempts) {
            try {
                jedisPool!!.use { pool ->
                    pool.resource.use { jedis ->
                        result = block(jedis)
                        return result
                    }
                }
            } catch (e: JedisConnectionException) {
                lastException = e
                if (attempt < maxRetryAttempts) {
                    plugin.logger.warning("Redis connection failed (attempt $attempt/$maxRetryAttempts). Retrying in ${retryDelayMs}ms...")
                    Thread.sleep(retryDelayMs.toLong())
                    connect()
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Redis operation '$operation' failed: ${e.message}", e)
                return null
            }
        }

        plugin.logger.severe("Failed to execute Redis operation '$operation' after $maxRetryAttempts attempts")
        lastException?.let { plugin.logger.log(Level.SEVERE, "Last exception:", it) }
        return null
    }
}