plugins {
    `java-library`
    `maven-publish`
}

group = "de.thepixel3261"
version = rootProject.version

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "api"
            version = project.version.toString()
        }
    }
}