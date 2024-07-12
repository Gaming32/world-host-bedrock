architectury {
    platformSetupLoomIde()
    neoForge()
}

val common by configurations.creating
val shadowCommon by configurations.creating

configurations {
    val developmentNeoForge by getting

    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    developmentNeoForge.extendsFrom(common)
}

dependencies {
    neoForge(libs.neoforge)

    common(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowCommon(project(":common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    from(shadowCommon.files.map { if (it.isDirectory) it else zipTree(it) })
    archiveClassifier = "dev"
}

tasks.remapJar {
    archiveClassifier = null
}

tasks.sourcesJar {
    val commonSources by project(":common").tasks.sourcesJar
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}
