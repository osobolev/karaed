plugins {
    id("lib")
}

dependencies {
    api(project(":run-util"))

    implementation(libs.gson)
    implementation(project(":json-util"))
}
