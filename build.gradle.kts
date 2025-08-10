plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "de.thepixel3261"
version = File("src/main/resources/plugin.yml").readText(Charsets.UTF_8).substringAfter("version: ").substringBefore("\n").replace("'", "")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("redis.clients:jedis:4.4.3")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
    }
}

tasks {
    shadowJar {
        relocate("org.bstats", "de.thepixel3261.momentum")
        archiveFileName.set("Momentum-$version.jar")
    }
    build {
        dependsOn(shadowJar)
    }
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
