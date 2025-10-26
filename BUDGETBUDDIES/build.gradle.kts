plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "org.example"
version = "1.0.SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

application {
    mainModule.set("org.example.budgetbuddies")
    mainClass.set("org.example.budgetbuddies.BudgetBuddyApp")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing","javafx.media")
}

dependencies {
    // BootstrapFX for beautiful styling
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")

    // ZXing for QR code generation and reading
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // Webcam capture for QR scanning
    implementation("com.github.sarxos:webcam-capture:0.3.12")

    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Configure the run task
tasks.named<JavaExec>("run") {
    // Ensure the application can access files in the working directory
    workingDir = projectDir
}

jlink {
    imageZip.set(layout.buildDirectory.file("distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "BudgetBuddy"
    }
}

// Create a fat JAR with all dependencies
tasks.register<Jar>("fatJar") {
    manifest {
        attributes(
            "Main-Class" to "org.example.budgetbuddies.BudgetBuddyApp",
            "Implementation-Title" to "BudgetBuddy",
            "Implementation-Version" to project.version
        )
    }
    archiveBaseName.set("BudgetBuddy")
    archiveVersion.set(project.version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
}