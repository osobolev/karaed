plugins {
    id("application")
    id("com.github.ben-manes.versions") version "0.45.0"
}

group = "io.github.osobolev"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
    create("manual") {
        java.setSrcDirs(listOf("test"))
    }
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    options.release.set(17)
}

dependencies {
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    implementation("com.google.code.gson:gson:2.12.1")
}

configurations["manualImplementation"].extendsFrom(configurations["implementation"])
configurations["manualRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])
configurations["manualCompileOnly"].extendsFrom(configurations["compileOnly"])

dependencies {
    "manualImplementation"(sourceSets["main"].output)
}

application {
    mainClass.set("karaed.gui.KaraGui")
    mainModule.set("karaed")
}
