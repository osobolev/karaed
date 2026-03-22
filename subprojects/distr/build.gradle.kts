plugins {
    id("common")
}

val release = "1.0.0"

val app by configurations.creating

dependencies {
    app(project(":gui"))
}

tasks.register("appdir", Copy::class) {
    from(app)
    from("config")
    from("$rootDir") {
        include("scripts/**")
    }
    into(layout.buildDirectory.dir("distr"))
}

fun toolPath(tool: String): File {
    val service = project.extensions.getByType<JavaToolchainService>()
    val launcher = service.launcherFor(java.toolchain)
    return launcher.get().metadata.installationPath.file("bin/$tool").asFile
}

tasks.register("jre", Exec::class) {
    executable(toolPath("jlink"))
    args(
        "--vm", "client", 
        "--no-header-files", "--no-man-pages", "--strip-debug",
        "--compress", "zip-9",
        "--add-modules", "java.desktop,jdk.crypto.ec,jdk.localedata",
        "--include-locales", "en",
        "--output", "jre"
    )
}

tasks.register("createDistr", Exec::class) {
    dependsOn("appdir")
    executable(toolPath("jpackage"))
    args(
        "--type", "app-image",
        "-i", layout.buildDirectory.dir("distr").get(),
        "-d", "$rootDir/distr",
        "-n", "karaed",
        "--main-jar", "gui.jar",
        "--runtime-image", "jre",
        "--description", "Karaoke editor",
        "--app-version", release,
        "--icon", "karaed.ico",
        "--java-options", "-Dapp.rootDir=\$APPDIR",
        "--java-options", "-Xmx512m",
        "--java-options", "-Dsun.java2d.uiScale=1.0",
    )
}

tasks.clean {
    delete("$rootDir/distr")
}

tasks.register("distr") {
    dependsOn("clean", "createDistr")
}

tasks.register("releaseZip", Zip::class) {
    dependsOn("distr")
    val baseName = "karaed-$release-windows_amd64"
    archiveFileName.set("$baseName.zip")
    destinationDirectory.set(file("$rootDir/distr"))
    from("$rootDir/distr/karaed") {
        into(baseName)
    }
}

