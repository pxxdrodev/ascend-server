plugins {
    id("java-library")
}

group = "com.ascend.lobby.api"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "${rootProject.projectDir}/libs", "include" to listOf("*.jar"))))

    implementation(project(":core:api"))

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

tasks.test {
    useJUnitPlatform()
}
