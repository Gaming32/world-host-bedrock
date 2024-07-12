val common by configurations.creating

configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
}

dependencies {
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    common(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }

    modImplementation(libs.viafabricplus)
}

loom.runs {
    named("client") {
        isIdeConfigGenerated = true
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.jar {
    from(common.files.map { if (it.isDirectory) it else zipTree(it) })
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
