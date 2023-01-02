val typeIDE:String by project

plugins {
    id("java")
    id("net.thauvin.erik.gradle.semver") version "1.0.4"
    id("org.jetbrains.intellij") version "1.9.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson" ){
        version {
            strictly("2.9.1")
        }
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")

}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2021.3")
    type.set(typeIDE) // Target IDE Platform
    downloadSources.set(false)
    plugins.set(listOf(/* Plugin Dependencies */))
}


configurations.all {
    resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.compilerArgs = listOf("-Xlint:deprecation")
    }

    withType<Test> {
        useJUnitPlatform()
    }

    init {
        version = semver.version
        group = "org.ideplugins.yaml-pipeline-lint-plugin"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("231.*")
        changeNotes.set(
            """
    <ul>
    <li>0.0.2
        <ul>
        <li>Add link action to jump to plugin settings to add gitlab token</li>
        </ul>
    </li>
    <li>0.0.1
      <ul>
      <li>Initial version</li>
      </ul>
    </li>      
    </ul>            
        """.trimIndent()
        )

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
