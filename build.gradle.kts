import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    id("org.jetbrains.kotlin.plugin.serialization")  version "1.6.21"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    application
}



group = "com.mjb2k.imagemapbot"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.discord4j:discord4j-core:3.2.4")
    implementation( "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation( "org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.2")
    implementation("dev.dejvokep:boosted-yaml:1.3")
    implementation("org.bstats:bstats-bukkit:3.0.1")
    compileOnly("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")
    compileOnly("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.11.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.11.0")

    implementation("io.ktor:ktor-client-core-jvm:2.0.3")
    implementation("io.ktor:ktor-client-cio-jvm:2.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.3")
    implementation ("io.ktor:ktor-client-content-negotiation:2.0.3")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    //mainClass.set("MainKt")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("imagemap-discord-bot")
        archiveClassifier.set("")
        mergeServiceFiles()
        manifest {
            project.setProperty("mainClassName", "com.mjb2k.imagemapbot.ImageMapBot")
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

