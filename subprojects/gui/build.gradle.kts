plugins {
    id("lib")
}

dependencies {
    implementation(libs.diff)
    implementation(project(":json-util"))
    implementation(project(":project"))
}

tasks.jar {
    manifest {
        attributes(
            "Class-Path" to configurations.runtimeClasspath.map { conf -> conf.files.map { f -> f.name }.sorted().joinToString(" ") },
            "Main-Class" to "karaed.gui.Main"
        )
    }
}
