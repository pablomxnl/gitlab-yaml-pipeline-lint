val typeIDE:String by project

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.5.2"
    id("net.thauvin.erik.gradle.semver") version "1.0.4"
}

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2019.3.5")
    type.set("$typeIDE") // Target IDE Platform
    downloadSources.set(false);
    plugins.set(listOf(/* Plugin Dependencies */))

}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson" ){
        version {
            strictly("2.9.1")
        }
    }
}

configurations.all {
    resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.compilerArgs = listOf("-Xlint:deprecation")
    }

    init {
        version = semver.version
        group = "org.ideplugins.yaml-pipeline-lint-plugin"
    }

    patchPluginXml {
        sinceBuild.set("193.5662.53")
        untilBuild.set("231.*")
        changeNotes.set("0.0.1 Initial version")
    }

    signPlugin {
        certificateChain.set(System.getenv("JBM_CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("JBM_PRIVATE_KEY"))
        password.set(System.getenv("JBM_PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("JBM_PUBLISH_TOKEN"))
    }
}
