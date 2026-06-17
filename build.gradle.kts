plugins {
    java
    application
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass.set("Server")
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.register<JavaExec>("runClient") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Client")
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    with(tasks.jar.get() as CopySpec)
}
