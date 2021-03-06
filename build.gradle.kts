import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.github.secretx33"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://plugins.gradle.org/m2/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.mattstudios.me/artifactory/public")
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT") // Spigot API dependency
    compileOnly(fileTree("libs"))      // Spigot server dependency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.+")
    implementation("com.github.secretx33:secret-cfg-bukkit:1.0-SNAPSHOT")
    implementation("io.arrow-kt:arrow-core:0.13.+")
    val koin_version = "3.0.+"
    implementation("io.insert-koin:koin-core:$koin_version")
    testCompileOnly("io.insert-koin:koin-test:$koin_version")
    implementation("com.github.cryptomorin:XSeries:7.9.1.1")
    implementation("net.kyori:adventure-platform-bukkit:4.0.0-SNAPSHOT")
    implementation("me.mattstudios:triumph-msg-adventure:2.2.4-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.4")
    implementation("com.zaxxer:HikariCP:4.0.+")
}

tasks.test {
    useJUnitPlatform()
}

// Disables the normal jar task
tasks.jar { enabled = false }

// And enables shadowJar task
artifacts.archives(tasks.shadowJar)

tasks.shadowJar {
    archiveFileName.set(rootProject.name + ".jar")
    val dependencyPackage = "${rootProject.group}.dependencies.${rootProject.name.toLowerCase()}"
    relocate("com.zaxxer.hikari", "${dependencyPackage}.hikari")
    relocate("okio", ".${dependencyPackage}.moshi.okio")
    relocate("org.koin", "${dependencyPackage}.koin")
    relocate("org.slf4j", "${dependencyPackage}.slf4j")
    relocate("kotlin", "${dependencyPackage}.kotlin")
    relocate("kotlinx", "${dependencyPackage}.kotlinx")
    relocate("com.cryptomorin.xseries", "${dependencyPackage}.xseries")
    relocate("me.mattstudios.msg", "${dependencyPackage}.mfmsg")
    relocate("arrow", "${dependencyPackage}.arrow")
    relocate("io.kindedj", "${dependencyPackage}.arrow.kindedj")
    relocate("com.github.secretx33.secretcfg", "${dependencyPackage}.secretcfg")
    relocate("org.spongepowered.configurate", "${dependencyPackage}.secretcfg.configurate")
    relocate("org.yaml.snakeyaml", "${dependencyPackage}.secretcfg.snakeyaml")
    relocate("io.leangen.geantyref", "${dependencyPackage}.secretcfg.geantyref")
    relocate("net.kyori", "${dependencyPackage}.kyori")
    exclude("ScopeJVMKt.class")
    exclude("DebugProbesKt.bin")
    exclude("META-INF/**")
    exclude("org.jetbrains")
    exclude("org.intellij")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }

tasks.processResources {
    val main_class = "${project.group}.${project.name.toLowerCase()}.${project.name}"
    expand("name" to project.name, "version" to project.version, "mainClass" to main_class)
}
