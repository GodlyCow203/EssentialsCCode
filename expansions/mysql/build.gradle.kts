plugins {
    id("com.gradleup.shadow")
}

tasks.shadowJar {
    archiveBaseName = "MySQLDatabaseExpansion"
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":plugin:essc"))
    // Provided natively by Paper at runtime; needed only to compile against MiniMessage/Component.
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.shadowJar {
    // Relocate HikariCP to avoid conflicts with other plugins that bundle it.
    relocate("com.zaxxer.hikari", "net.godlycow.org.essc.expansion.mysql.libs.hikari")
    // The MySQL connector is intentionally NOT relocated so DriverManager SPI keeps working.
}
