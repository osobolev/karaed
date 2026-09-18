plugins {
    id("common")
    `java-library`
}

group = "io.github.osobolev.karaed"

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
    test {
        java.srcDir("unit")
        resources.srcDir("unitResources")
    }
    create("manual") {
        java.srcDir("test")
        resources.srcDir("testResources")
    }
}

configurations["manualImplementation"].extendsFrom(configurations["implementation"])
configurations["manualRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])
configurations["manualCompileOnly"].extendsFrom(configurations["compileOnly"])

dependencies {
    "manualImplementation"(sourceSets["main"].output)
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.clean {
    delete("$projectDir/out")
}
