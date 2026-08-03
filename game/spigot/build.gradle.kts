plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.ascend.game.spigot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "${rootProject.projectDir}/libs", "include" to listOf("*.jar"))))

    implementation(project(":game:api"))
    implementation(project(":core:api"))
    implementation(project(":core:spigot"))

    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("fr.mrmicky:fastboard:2.1.3")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("game-spigot")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles()
    destinationDirectory.set(layout.buildDirectory.dir("libs/game"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
