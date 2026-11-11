plugins {
    java
}

group = "com.thecottagestar"
version = "1.3.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Official repository for Geyser/Floodgate APIs and Cumulus forms.
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    // Paper 26.2 is the target server line. Paper provides this API at runtime.
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    // Floodgate provides Bedrock detection and the Cumulus Forms API at runtime.
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("CottageTPAGUI")
}
