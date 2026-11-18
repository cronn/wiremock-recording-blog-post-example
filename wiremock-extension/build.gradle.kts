import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("com.gradleup.shadow")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Der WireMock-Container läuft mit Java 17
    options.release.set(17)
}

dependencies {
    compileOnly("org.wiremock:wiremock:3.13.2")
    testImplementation("org.wiremock:wiremock:3.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("overpass-wiremock-extension")
    archiveClassifier.set("")
}

tasks.register<Copy>("copyToWireMock") {
    description = "Kopiert die Extension in das Verzeichnis, das in den WireMock-Container gemountet wird."
    from(tasks.named("shadowJar"))
    into(rootProject.layout.projectDirectory.dir("wiremock/extensions"))
}
