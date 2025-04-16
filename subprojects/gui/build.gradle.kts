plugins {
    id("lib")
}

dependencies {
    implementation(libs.diff)
    implementation(project(":json-util"))
    implementation(project(":project"))
}
