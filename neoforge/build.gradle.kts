val common by configurations.creating

configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
}

dependencies {
    neoForge(libs.neoforge)

    common(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
}

loom.runs {
    named("client") {
        isIdeConfigGenerated = true
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
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
