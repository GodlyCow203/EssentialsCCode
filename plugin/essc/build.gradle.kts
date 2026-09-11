plugins {
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

tasks.shadowJar {
    archiveBaseName = "EssentialsC"
}

tasks.runServer {
    minecraftVersion("1.20.6")
    jvmArgs("-Xms1G", "-Xmx1G")
}

tasks.shadowJar {
    relocate("dev.faststats", "net.godlycow.org.essc.libs.faststats")
    relocate("org.bstats", "net.godlycow.org.essc.libs.bstats")
}

repositories {
    maven("https://jitpack.io")
    maven("https://nexus.scarsz.me/content/groups/public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.faststats.dev/releases")
    maven("https://repo.helpch.at/releases/")
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

dependencies {
    api(project(":api"))

    compileOnly("com.discordsrv:discordsrv:1.26.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("org.bukkit", "bukkit")
    }
    compileOnly("com.github.NEZNAMY:TAB-API:5.5.0")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.0.12")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
    implementation("de.rapha149.signgui:signgui:2.5.3")
    implementation("dev.faststats.metrics:bukkit:0.27.1")
    implementation("org.bstats:bstats-bukkit:3.1.0")
}
