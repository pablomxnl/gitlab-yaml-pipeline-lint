import org.jetbrains.intellij.tasks.RunIdeTask
import org.jsoup.Jsoup

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

group = properties("pluginGroup").get()


plugins {
    id("java")
    id("jacoco")
    alias(libs.plugins.asciidoc)
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.semver)
    alias(libs.plugins.jacocolog)
}

configurations.all {
    resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
}

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version = properties("platformVersion")
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
    updateSinceUntilBuild = false
    type = properties("platformType") // Target IDE Platform
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.sentrysdk){
        exclude(group = "org.slf4j")
    }

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver){
        exclude("junit")
    }
    testImplementation(libs.mockito)

    testRuntimeOnly(libs.junitplatform)
    testRuntimeOnly(libs.junitengine)

}

val dir1 = file("${intellij.sandboxDir.get()}/manualtest")
val dirConfig = file("${intellij.sandboxDir.get()}/manualtest/config")
val dirSystem = file("${intellij.sandboxDir.get()}/manualtest/system")

tasks.register("createDirsManualTesting"){
    mkdir(dir1.toPath())
    mkdir(dirConfig.toPath())
    mkdir(dirSystem.toPath())
}

tasks.register<RunIdeTask>("runForManualTests"){
    dependsOn("createDirsManualTesting")
    doFirst{
        copy{
            from("${projectDir}/src/test/resources/ide/options/")
            into("${dirConfig}/options/")
            include("*.xml")
        }
        copy{
            from("${projectDir}/src/test/resources/ide/options/inspectionProfiles")
            into("${intellij.sandboxDir.get()}/config/options/")
        }
    }
    configDir = dirConfig
    systemDir = dirSystem
    systemProperty("idea.auto.reload.plugins", "false")
    systemProperty("idea.trust.all.projects", "true")
    systemProperty("ide.show.tips.on.startup.default.value", "false")
    args = listOf("${projectDir}/src/test/resources/annotator/")
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
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.compilerArgs = listOf("-Xlint:deprecation","-Xlint:unchecked")
    }

    withType<Test> {
        useJUnitPlatform()
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
        finalizedBy("jacocoTestReport")
    }

    init {
        version = semver.version
    }

    asciidoctor {
        dependsOn(processTestResources)
        setSourceDir(baseDir)
        sources {
            include("CHANGELOG.adoc")
        }
        setOutputDir(file("build/docs"))
    }

    patchPluginXml {
        dependsOn("asciidoctor")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")
        changeNotes = provider {
            Jsoup.parse(file("build/docs/CHANGELOG.html"))
                .select("#releasenotes")[0].nextElementSibling()?.children()
                ?.toString()
        }
    }

    signPlugin {
        certificateChain = environment("JBM_CERTIFICATE_CHAIN")
        privateKey = environment("JBM_PRIVATE_KEY")
        password = environment("JBM_PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        token = environment("JBM_PUBLISH_TOKEN")
        if (semver.preRelease.contains("SNAPSHOT")) {
            channels = listOf("EAP")
        }
    }

    jacocoTestReport {
        classDirectories.setFrom(instrumentCode)
        reports {
            xml.required = true
        }
    }

    runIde {
        doFirst{
            copy{
                from("${projectDir}/src/test/resources/ide/options/")
                into("${intellij.sandboxDir.get()}/config/options/")
                include("*.xml")
            }
            copy{
                from("${projectDir}/src/test/resources/ide/options/inspectionProfiles")
                into("${intellij.sandboxDir.get()}/config/options/")
            }
        }
        systemProperty("idea.auto.reload.plugins", "false")
        systemProperty("idea.trust.all.projects", "true")
        systemProperty("ide.show.tips.on.startup.default.value", "false")
    }

}
