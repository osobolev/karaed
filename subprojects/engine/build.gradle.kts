plugins {
    id("lib")
}

dependencies {
    api(project(":run-util"))

    implementation(project(":json-util"))
    implementation(project(":java-ass"))
}
