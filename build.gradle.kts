plugins {
    id("fabric-loom") version "1.17.20"
}

val modId = property("mod.id") as String
val modVersion = property("mod.version") as String
val minecraftVersion = stonecutter.current.version

val javaVersion = if (stonecutter.current.parsed >= "1.20.6") 21 else 17

version = "$modVersion+$minecraftVersion"
group = property("mod.group") as String

base {
    archivesName = modId
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
}

val templateProperties = mapOf(
    "version" to version.toString(),
    "minecraft" to property("deps.minecraft_range").toString(),
    "fabricloader" to property("deps.fabric_loader").toString(),
    "java" to javaVersion.toString()
)

tasks.processResources {
    filteringCharset = "UTF-8"
    inputs.properties(templateProperties)
    filesMatching("fabric.mod.json") {
        expand(templateProperties)
    }
}
