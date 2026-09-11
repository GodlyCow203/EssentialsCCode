import java.net.URI

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.3" apply false
}

group = "net.godlycow.org"

allprojects {
    group = rootProject.group
    version = rootProject.version

    apply {
        plugin("java")
        plugin("java-library")
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(21)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    // paper still resolves against 21
    configurations.named("compileClasspath") {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }

    tasks.named<ProcessResources>("processResources") {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("project" to mapOf("version" to project.version))
        }
    }

    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    }
}

val essentialsCProjectPath = ":plugin:essc"
val testProjectPath = ":plugin:test"
val essentialsCJarBaseName = "EssentialsC"
val testJarBaseName = "EssentialsCTest"
val serversDirectory = layout.projectDirectory.dir("servers")
val pluginsSubdirectoryName = "plugins"

//java paths can be overridden in gradle.properties
val java21Path = providers.gradleProperty("minecraft.java21")
    .orElse("java")
    .get()
val java25Path = providers.gradleProperty("minecraft.java25")
    .orElse("C:/Program Files/Java/jdk-25/bin/java") //adjust if needed
    .get()

enum class MinecraftServerType(val papermcProjectName: String) {
    PAPER("paper"),
    FOLIA("folia")
}

data class MinecraftServerDefinition(
    val identifier: String,
    val serverType: MinecraftServerType,
    val javaPath: String
)

//folia and paper are on 26.+ so they require java 25
val minecraftServerDefinitions = listOf(
    MinecraftServerDefinition("paper-26.2", MinecraftServerType.PAPER, java25Path),
    MinecraftServerDefinition("paper-1.20.6", MinecraftServerType.PAPER, java21Path),
    MinecraftServerDefinition("folia-26.1.2", MinecraftServerType.FOLIA, java25Path)
)

minecraftServerDefinitions.forEach { serverDefinition ->
    val serverDirectory = serversDirectory.dir(serverDefinition.identifier).asFile
    val serverJarFile = serverDirectory.resolve("server.jar")
    val serverPluginsDirectory = serverDirectory.resolve(pluginsSubdirectoryName)
    val taskNameSuffix = serverDefinition.identifier
        .split("-", ".")
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }

    val deployTask = tasks.register<Copy>("deployEssentialsCTo$taskNameSuffix") {
        group = "minecraft servers"
        description = "Copies the freshly built EssentialsC jar to ${serverDefinition.identifier}."
        dependsOn("$essentialsCProjectPath:shadowJar")

        doFirst {
            serverPluginsDirectory.mkdirs()

            //remove old jars first
            fileTree(serverPluginsDirectory) {
                include("$essentialsCJarBaseName-*.jar")
            }.forEach { it.delete() }

            serverPluginsDirectory.resolve(essentialsCJarBaseName).deleteRecursively()
        }

        from(project(essentialsCProjectPath).tasks.named<Jar>("shadowJar").get().outputs.files)
        into(serverPluginsDirectory)
    }

    val deployTestTask = tasks.register<Copy>("deployTestPluginTo$taskNameSuffix") {
        group = "minecraft servers"
        description = "Copies the EssentialsCTest jar to ${serverDefinition.identifier}."
        dependsOn("$testProjectPath:shadowJar")

        doFirst {
            serverPluginsDirectory.mkdirs()

            //remove old test jars first
            fileTree(serverPluginsDirectory) {
                include("$testJarBaseName-*.jar")
            }.forEach { it.delete() }
        }

        from(project(testProjectPath).tasks.named<Jar>("shadowJar").get().outputs.files)
        into(serverPluginsDirectory)
    }

    tasks.register<Exec>("run$taskNameSuffix") {
        group = "minecraft servers"
        description = "Starts the ${serverDefinition.identifier} server."
        dependsOn(deployTask, deployTestTask)

        workingDir = serverDirectory
        commandLine(serverDefinition.javaPath, "-jar", serverJarFile.absolutePath, "--nogui")

        //allow typing commands in console
        standardInput = System.`in`
        standardOutput = System.out
        errorOutput = System.err
    }

    tasks.register<Delete>("cleanupTestPluginFrom$taskNameSuffix") {
        group = "minecraft servers"
        description = "Removes EssentialsCTest jar from ${serverDefinition.identifier}"
        delete(fileTree(serverPluginsDirectory) {
            include("$testJarBaseName-*.jar")
        })
    }
}

// globla cleanup task
tasks.register<Delete>("cleanupAllTestPlugins") {
    group = "minecraft servers"
    description = "Removes EssentialsCTest jar from all server plugins folders"
    minecraftServerDefinitions.forEach { serverDefinition ->
        val serverPluginsDirectory = serversDirectory.dir(serverDefinition.identifier)
            .dir(pluginsSubdirectoryName).asFile
        delete(fileTree(serverPluginsDirectory) {
            include("$testJarBaseName-*.jar")
        })
    }
}