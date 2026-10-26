plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.awesomeshot5051"
version = "1.2.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

javafx {
    version = "25.0.4"
    modules = listOf("javafx.controls")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "com.awesomeshot5051.edl2chapters.Launcher"
}

tasks.test {
    useJUnitPlatform()
}

// ---- Debian package (jpackage, bundles its own Java runtime) ----
val packageName = "edl2chapters"
val jpackageInput = layout.buildDirectory.dir("jpackage/input")
val debOutput = layout.buildDirectory.dir("distributions")

val prepareJpackageInput = tasks.register<Sync>("prepareJpackageInput") {
    description = "Collects the application and JavaFX jars for jpackage."
    group = "distribution"
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    into(jpackageInput)
}

val buildDeb = tasks.register<Exec>("buildDeb") {
    description = "Builds a .deb installer containing a private JRE (needs dpkg-deb and fakeroot)."
    group = "distribution"
    dependsOn(prepareJpackageInput)

    // jpackage ships with full JDKs only (the JetBrains Runtime that IDEs use for Gradle lacks it).
    // Order: -PjdkHome=<path>, the Gradle JVM, then jpackage on PATH.
    val jpackageBinary = listOfNotNull(
        providers.gradleProperty("jdkHome").orNull,
        System.getProperty("java.home")
    ).map { File(it, "bin/jpackage") }.firstOrNull { it.exists() }
        ?: System.getenv("PATH").split(File.pathSeparator).map { File(it, "jpackage") }.firstOrNull { it.exists() }
    val jarName = tasks.jar.flatMap { it.archiveFileName }
    val appVersion = project.version.toString()

    doFirst {
        check(jpackageBinary != null) { "jpackage not found; pass -PjdkHome=<full JDK 25 path> or put it on PATH" }
        debOutput.get().asFile.mkdirs()
    }
    inputs.dir(jpackageInput)
    outputs.dir(debOutput)

    commandLine(
        jpackageBinary!!.absolutePath,
        "--type", "deb",
        "--name", packageName,
        "--app-version", appVersion,
        "--vendor", "awesomeshot5051",
        "--description", "Converts DaVinci Resolve EDL markers to YouTube chapters",
        "--input", jpackageInput.get().asFile.absolutePath,
        "--main-jar", jarName.get(),
        "--main-class", "com.awesomeshot5051.edl2chapters.Launcher",
        "--add-modules", "java.base,java.desktop,java.logging,java.xml,jdk.unsupported",
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--icon", file("src/main/resources/icons/ETIcon.png").absolutePath,
        "--linux-shortcut",
        "--linux-menu-group", "AudioVideo;Video",
        "--linux-package-name", packageName,
        "--dest", debOutput.get().asFile.absolutePath
    )
}

// jpackage only installs under /opt; repack the .deb to add /usr/bin/edl2chapters (CLI on the user's PATH).
// The same launcher opens the GUI with no arguments and runs the CLI with a file argument.
val addCliToPath = tasks.register<Exec>("addCliToPath") {
    description = "Adds a /usr/bin symlink to the generated .deb."
    group = "distribution"
    dependsOn(buildDeb)
    val deb = debOutput.map { it.file("${packageName}_${project.version}_amd64.deb").asFile }
    val work = layout.buildDirectory.dir("deb-repack")
    doFirst { work.get().asFile.deleteRecursively() }
    commandLine(
        "sh", "-c",
        """
        set -e
        W="${'$'}1"; D="${'$'}2"
        dpkg-deb -R "${'$'}D" "${'$'}W"
        mkdir -p "${'$'}W/usr/bin"
        chmod 755 "${'$'}W/usr" "${'$'}W/usr/bin"
        sed -i 's/^Name=.*/Name=Edl2Chapters/' "${'$'}W"/opt/$packageName/lib/$packageName-$packageName.desktop
        echo "StartupWMClass=com.awesomeshot5051.edl2chapters.app.gui.EdlToChaptersApp" >> "${'$'}W"/opt/$packageName/lib/$packageName-$packageName.desktop
        ln -sf /opt/$packageName/bin/$packageName "${'$'}W/usr/bin/$packageName"
        fakeroot dpkg-deb -b "${'$'}W" "${'$'}D"
        """.trimIndent(),
        "sh", work.get().asFile.absolutePath, deb.get().absolutePath
    )
}

tasks.register("packageDeb") {
    description = "Builds the final installable .deb (GUI launcher, icon, CLI on PATH)."
    group = "distribution"
    dependsOn(addCliToPath)
}
