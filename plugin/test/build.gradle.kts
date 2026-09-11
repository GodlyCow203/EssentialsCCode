plugins {
    id("com.gradleup.shadow")
}

tasks.shadowJar {
    archiveBaseName = "EssentialsCTest"
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":plugin:essc"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}
