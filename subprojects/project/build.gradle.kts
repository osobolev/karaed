plugins {
    id("lib")
}

dependencies {
    api(project(":workdir"))
    api(project(":engine"))

    implementation(project(":json-util"))
}
