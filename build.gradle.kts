val typeIDE:String by project

plugins {
    id("java")
    id("net.thauvin.erik.gradle.semver") version "1.0.4"
    id("org.jetbrains.intellij") version "1.13.1"
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
    id("jacoco")
}

configurations.all {
    resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
}

val ciEnvVar: String? = System.getenv("CI")
val isInCI: Boolean = ciEnvVar?.isNotEmpty() ?: false

repositories {
    mavenCentral()
    maven {
        name = "GITLAB_MAVEN"
        setUrl("https://gitlab.com/api/v4/projects/44083372/packages/maven")
        credentials(HttpHeaderCredentials::class) {
            name = if (isInCI) "Job-Token" else "Private-Token"
            value = if (isInCI) System.getenv("CI_JOB_TOKEN") else System.getenv("GITLAB_TOKEN")
        }
        authentication {
            create("header", HttpHeaderAuthentication::class)
        }
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2021.3.3")
    type.set(typeIDE) // Target IDE Platform
    updateSinceUntilBuild.set(false)
    plugins.set(listOf("java", "org.jetbrains.plugins.yaml:213.6777.22"))
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0"){
        exclude("junit")
    }
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    implementation("org.ideplugins:pluginSettingsLibrary:0.0.1")
    implementation("io.sentry:sentry:6.17.0"){
        exclude(group = "org.slf4j")
    }
}

tasks.register<JavaExec>("FetchGitlabVariables") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.ideplugins.gitlab_pipeline_lint.gitlab.FetchGitlabVariables")
    setArgsString(file("${projectDir}/src/main/resources/gitlab-variables.json").path)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.compilerArgs = listOf("-Xlint:deprecation","-Xlint:unchecked")
    }

    withType<Test> {
        useJUnitPlatform()
        finalizedBy("jacocoTestReport")
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
    <li>0.0.6
        <ul>
        <li>Adjust to use new pipeline lint endpoint per project as old global endpoint is removed in Gitlab 16.0 </li>
        </ul>
    </li>       
    <li>0.0.5
        <ul>
        <li>Add autocomplete for gitlab variables</li>
        </ul>
    </li>      
    <li>0.0.4
        <ul>
        <li>Annotating the results in the problem view / editor </li>
        <li>Improve error reporting to include warnings</li>
        <li>Add uncaught error reporting</li>
        <li>Plugin update notification begging for rate/review 🤣 </li>
        </ul>
    </li>    
    <li>0.0.3
        <ul>
        <li>Minor changes displaying lint results</li>
        </ul>
    </li>
    
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

    jacocoTestReport {
        reports {
            xml.required.set(true)
        }
    }

//    runPluginVerifier {
//        ideVersions.set(listOf("IU-231.8109.175","IC-213.7172.25"))
//    }
}
