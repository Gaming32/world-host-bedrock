import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    java
    alias(libs.plugins.architectury.loom) apply false
//    alias(libs.plugins.minotaur) apply false // TODO: Publishing
}

operator fun Project.get(key: String) = properties[key] as String

group = "io.github.gaming32"
version = rootProject["mod_version"]

allprojects {
    apply(plugin = "java")

    base.archivesName = rootProject.name
    group = rootProject.group

    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.lenni0451.net/everything")
        maven("https://api.modrinth.com/maven")
        maven("https://maven.neoforged.net/releases")
        maven("https://repo.opencollab.dev/main")
        maven("https://maven.parchmentmc.org")
        maven("https://repo.viaversion.com/everything")

        maven("https://jitpack.io")

        if (rootProject["local_world_host"].toBoolean()) {
            mavenLocal()
        }
    }

    tasks.withType<JavaCompile> {
        options.release = 21
    }

    java {
        withSourcesJar()
    }

    tasks.processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    val loom = extensions.getByName("loom") as LoomGradleExtensionAPI
    val include = configurations["include"]
    val mappings = configurations["mappings"]
    val minecraft = configurations["minecraft"]
    val modImplementation = configurations["modImplementation"]

    val libs = rootProject.libs

    version = "${rootProject.version}+$name"

    dependencies {
        minecraft(libs.minecraft)
        @Suppress("UnstableApiUsage")
        mappings(loom.layered {
            officialMojangMappings {
                nameSyntheticMembers = true
            }
            parchment(libs.parchment)
        })

        val worldHostVersion = "${rootProject["world_host_version"]}+${libs.minecraft.get().version}-${loom.platform.get().id()}"
        if (rootProject["local_world_host"].toBoolean()) {
            modImplementation("io.github.gaming32:world-host:$worldHostVersion")
        } else {
            modImplementation("maven.modrinth:world-host:$worldHostVersion")
        }

        implementation(libs.minecraftauth)
        include(libs.minecraftauth)

        implementation(libs.geyser.api)

        compileOnly(libs.mcdev.annotations)
    }
}
