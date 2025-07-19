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

tasks.named("clean").configure {
    doLast {
        project.delete("$rootDir/distr")
    }
}

tasks.register("distr", Copy::class) {
    from(configurations.runtimeClasspath)
    from(tasks.jar)
    from("$rootDir/config")
    from("$rootDir") {
        include("scripts/**")
    }
    into("$rootDir/distr")
}
