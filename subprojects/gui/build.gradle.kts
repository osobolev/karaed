plugins {
    id("lib")
}

dependencies {
    api("com.google.code.gson:gson:2.12.1")

    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    implementation(project(":run-util"))
    implementation(project(":json-util"))
    implementation(project(":workdir"))
}
