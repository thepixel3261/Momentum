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
 * See LICENSE (bottom) for full additional terms.
 * See plugin.yml for full notice.
 */


package de.thepixel3261.momentum.redis

import com.google.gson.Gson
import de.thepixel3261.momentum.Main
import de.thepixel3261.momentum.config.ConfigLoader
import de.thepixel3261.momentum.session.SessionData
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

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

            // For SSL connection with authentication
            if (configLoader.redisSsl) {
                val sslSocketFactory = SSLContext.getDefault().socketFactory
                val sslParameters = SSLParameters().apply {
                    endpointIdentificationAlgorithm = if (configLoader.redisSslVerifyPeer) "HTTPS" else null
                }
                val hostnameVerifier = if (configLoader.redisSslVerifyPeer) {
                    HttpsURLConnection.getDefaultHostnameVerifier()
                } else {
                    HostnameVerifier { _, _ -> true }
                }

                jedisPool = if (configLoader.redisUser.isNotEmpty() || configLoader.redisPassword.isNotEmpty()) {
                    // With authentication
                    JedisPool(
                        poolConfig,
                        configLoader.redisHost,
                        configLoader.redisPort,
                        2000,
                        configLoader.redisPassword,
                        0,
                        configLoader.redisUser,
                        true,
                        sslSocketFactory,
                        sslParameters,
                        hostnameVerifier
                    )
                } else {
                    // Without authentication
                    JedisPool(
                        poolConfig,
                        URI(configLoader.redisHost),
                        configLoader.redisPort,
                        sslSocketFactory,
                        sslParameters,
                        hostnameVerifier
                    )
                }
            } else {
                // Non-SSL connection
                jedisPool = if (configLoader.redisUser.isNotEmpty() || configLoader.redisPassword.isNotEmpty()) {
                    JedisPool(
                        poolConfig,
                        configLoader.redisHost,
                        configLoader.redisPort,
                        configLoader.redisUser,
                        configLoader.redisPassword,
                    )
                } else {
                    JedisPool(
                        poolConfig,
                        configLoader.redisHost,
                        configLoader.redisPort
                    )
                }
            }
            
            plugin.logger.info("Successfully connected to Redis${if (configLoader.redisSsl) " with SSL" else ""}.")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to connect to Redis: ${e.message}")
            e.printStackTrace()
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