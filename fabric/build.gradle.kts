architectury {
    platformSetupLoomIde()
    fabric()
}

val common by configurations.creating
val shadowCommon by configurations.creating

configurations {
    val developmentFabric by getting

    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    developmentFabric.extendsFrom(common)
}

dependencies {
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    common(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowCommon(project(":common", configuration = "transformProductionFabric")) {
        isTransitive = false
    }

    modImplementation(libs.viafabricplus)
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
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
