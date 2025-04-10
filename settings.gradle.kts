plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

rootProject.name = "karaed"

include("java-ass")
include("io-util")
include("json-util")
include("gui-manual")
