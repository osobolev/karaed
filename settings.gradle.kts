plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

rootProject.name = "karaed"

fun add(name: String) {
    val mname = "subprojects/$name"
    include(mname)
    project(":$mname").name = name
}


add("java-ass")
add("run-util")
add("json-util")
add("workdir")
add("engine")
add("gui")
