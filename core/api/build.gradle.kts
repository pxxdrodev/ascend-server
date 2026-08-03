plugins {
    id("java-library")
}

group = "com.ascend.core.api"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "${rootProject.projectDir}/libs", "include" to listOf("*.jar"))))

    api("org.mongodb:mongodb-driver-sync:4.11.1")
    api("redis.clients:jedis:5.1.0")

    api("org.slf4j:slf4j-api:2.0.9")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

tasks.test {
    useJUnitPlatform()
}
