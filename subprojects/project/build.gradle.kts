plugins {
    id("lib")
}

dependencies {
    api(project(":engine"))

    implementation(project(":json-util"))
}
