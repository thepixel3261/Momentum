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


package de.thepixel3261.momentum.core.redis

import com.google.gson.Gson
import de.thepixel3261.momentum.core.Main
import de.thepixel3261.momentum.core.config.ConfigLoader
import de.thepixel3261.momentum.core.session.SessionData
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

class RedisManager(private val plugin: Main, private val configLoader: ConfigLoader) {
    var jedisPool: JedisPool? = null
    private val gson = Gson()
    private val ioExecutor: ExecutorService = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "Momentum-Redis-IO").apply { isDaemon = true }
    }

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

            jedisPool!!.resource.use { jedis ->
                jedis.ping()
            }

            plugin.logger.info("Successfully connected to Redis${if (configLoader.redisSsl) " with SSL" else ""}.")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to connect to Redis: ${e.message}")
            e.printStackTrace()
            jedisPool = null
        }
    }

    fun isLeavingAsync(uuid: UUID, callback: (Boolean) -> Unit) {
        if (jedisPool == null) {
            callback(false); return
        }
        ioExecutor.submit {
            val result = try {
                jedisPool!!.resource.use { jedis ->
                    jedis.exists("momentum:leaving:$uuid")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Redis error: ${e.message}")
                false
            }
            callback(result)
        }
    }

    fun setLeavingAsync(data: SessionData) {
        if (jedisPool == null) return
        ioExecutor.submit {
            try {
                jedisPool!!.resource.use { jedis ->
                    jedis.setex("momentum:leaving:${data.uuid}", 30, "true")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Redis error: ${e.message}")
            }
            saveSessionDataAsync(data)
        }
    }

    fun clearLeavingAsync(uuid: UUID) {
        if (jedisPool == null) return
        ioExecutor.submit {
            try {
                jedisPool!!.resource.use { jedis ->
                    jedis.del("momentum:leaving:$uuid")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Redis error: ${e.message}")
            }
        }
    }

    private fun saveSessionDataAsync(session: SessionData) {
        if (jedisPool == null) return
        ioExecutor.submit {
            try {
                val json = gson.toJson(session)
                jedisPool!!.resource.use { jedis ->
                    jedis.setex("momentum:session:${session.uuid}", 30, json)
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to save session data: ${e.message}")
            }
        }
    }

    fun getSessionDataAsync(uuid: UUID, callback: (SessionData?) -> Unit) {
        if (jedisPool == null) {
            callback(null); return
        }
        ioExecutor.submit {
            val result: SessionData? = try {
                jedisPool!!.resource.use { jedis ->
                    val json = jedis.get("momentum:session:$uuid") ?: return@submit callback(null)
                    gson.fromJson(json, SessionData::class.java)
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load session data: ${e.message}")
                null
            }
            callback(result)
        }
    }

    fun clearSessionDataAsync(uuid: UUID) {
        if (jedisPool == null) return
        ioExecutor.submit {
            try {
                jedisPool!!.resource.use { jedis ->
                    jedis.del("momentum:session:$uuid")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to clear session data: ${e.message}")
            }
        }
    }

    fun disconnect() {
        jedisPool?.close()
        ioExecutor.shutdownNow()
    }
}