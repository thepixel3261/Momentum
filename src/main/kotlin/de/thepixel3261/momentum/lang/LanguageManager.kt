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


package de.thepixel3261.momentum.lang

import de.thepixel3261.momentum.Main
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class LanguageManager (val plugin: Main) {
    private var lang: String = plugin.configLoader.lang
    var langMap: HashMap<String, String> = HashMap()
    val langDir = File(plugin.dataFolder, "/languages")
    init {
        lang = plugin.configLoader.lang
        LanguageParser.init(this)
        try {
            if (!langDir.exists()) {
                langDir.mkdir()
            }

            saveDefaultLangs()

            val langFile = File(langDir, "$lang.yml")

            val config = YamlConfiguration.loadConfiguration(langFile)

            loadLangs(config)
        } catch (_: Exception) {
            plugin.logger.warning("Unable to load language file: /languages/$lang.yml")
        }
    }

    fun loadLangs(file: YamlConfiguration) {
        langMap.clear()
        processSection(file, "")
    }

    private fun processSection(section: org.bukkit.configuration.ConfigurationSection, parentKey: String) {
        for (key in section.getKeys(false)) {
            val fullKey = if (parentKey.isEmpty()) key else "$parentKey.$key"

            if (section.isConfigurationSection(key)) {
                // If this is a section, process it recursively
                processSection(section.getConfigurationSection(key)!!, fullKey)
            } else {
                // If this is a value, add it to the map
                langMap[fullKey] = section.getString(key, "")
            }
        }
    }

    fun saveDefaultLangs() {
        val enLangFile = File(langDir, "en_US.yml")
        if (!enLangFile.exists()) {
            plugin.saveResource("languages/en_US.yml", false)
        }

        val deLangFile = File(langDir, "de_DE.yml")
        if (!deLangFile.exists()) {
            plugin.saveResource("languages/de_DE.yml", false)
        }
    }
}
