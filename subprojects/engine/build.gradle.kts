plugins {
    id("lib")
}

dependencies {
    api(project(":run-util"))

    implementation("com.google.code.gson:gson:2.12.1")
    implementation(project(":json-util"))
}
