plugins {
    java
    application
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass.set("Server")
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runClient") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Client")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register("fatJar") {
    dependsOn(tasks.jar)
}

val graalVmHome = providers.gradleProperty("graalVmHome")
    .orElse("/usr/lib/jvm/java-25-graalvm")

fun registerNativeImageTask(taskName: String, mainClassName: String, outputName: String) {
    tasks.register<Exec>(taskName) {
        group = "build"
        description = "Builds the $outputName native binary with GraalVM native-image."
        dependsOn(tasks.classes)

        val nativeBuildDir = layout.buildDirectory.dir("native").get().asFile
        val binaryFile = nativeBuildDir.resolve(outputName)
        val tempDir = nativeBuildDir.resolve("tmp/$outputName")

        inputs.files(sourceSets["main"].runtimeClasspath)
        outputs.file(binaryFile)

        doFirst {
            nativeBuildDir.mkdirs()
            tempDir.mkdirs()
        }

        environment("TMPDIR", tempDir.absolutePath)
        environment("TMP", tempDir.absolutePath)
        environment("TEMP", tempDir.absolutePath)

        executable = graalVmHome.map { "$it/bin/native-image" }.get()
        args(
            "-cp", sourceSets["main"].runtimeClasspath.asPath,
            "--no-fallback",
            "-J-Djava.io.tmpdir=${tempDir.absolutePath}",
            "-J-XX:-UsePerfData",
            "-H:TempDirectory=${tempDir.absolutePath}",
            "-H:Class=$mainClassName",
            "-o", binaryFile.absolutePath
        )
    }
}

registerNativeImageTask("nativeServer", "Server", "java-chat-server")
registerNativeImageTask("nativeClient", "Client", "java-chat-client")
registerNativeImageTask("nativeSend", "SimpleClient", "java-chat-send")

tasks.register("nativeCompile") {
    group = "build"
    description = "Builds all native binaries."
    dependsOn("nativeServer", "nativeClient", "nativeSend")
}
