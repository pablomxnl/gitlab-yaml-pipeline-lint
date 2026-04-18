import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jsoup.Jsoup

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

group = properties("pluginGroup").get()

plugins {
    id("java")
    id("jacoco")
    alias(libs.plugins.asciidoc)
    alias(libs.plugins.gradleIntelliJPlugin)
    alias(libs.plugins.semver)
    alias(libs.plugins.kotlin)
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

asciidoc {
    publications {
        named("main") {
            sourceSet {
                setSourceDir(project.projectDir.path)
                sources {
                    include("changelog.adoc")
                }
            }
            output("asciidoctorj", "html")
        }
    }
}

tasks.named("asciidoctorHtml"){
    dependsOn(listOf("generateManifest","compileKotlin","compileJava"))
    doNotTrackState("doNotTrack")
    outputs.cacheIf { false }
}

@Suppress("unused")
val runIdeForManualTests by intellijPlatformTesting.runIde.registering {
    prepareSandboxTask {
        sandboxDirectory = project.layout.buildDirectory.dir("custom-sandbox")
        sandboxSuffix = ""
    }

    task {
        autoReload = true
        doFirst {
            copy {
                from("${projectDir}/src/test/resources/ide/options/")
                into(project.layout.buildDirectory.dir("custom-sandbox/config/options"))
                include("*.xml")
            }
        }
        systemProperty("idea.trust.all.projects", "true")
        systemProperty("ide.show.tips.on.startup.default.value", "false")
        systemProperty("idea.is.internal", "true")
        systemProperty("idea.disposer.debug", "on")
        systemProperty("nosplash", "true")
        args = listOf("${projectDir}/src/test/resources/annotator/")
    }
}

@Suppress("unused")
val runIdeEAP by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.IntellijIdea
    version = "253-EAP-SNAPSHOT"
    useInstaller = false
}

tasks.register("printCoverageForGitlab") {
    outputs.cacheIf { false }
    var report = file("build/reports/jacoco/test/html/index.html")
    if (report.exists()){
        var coverage = Jsoup.parse(report)
            .select("tfoot td")[2]?.text()
        print("    - Instruction Coverage: $coverage")
    }
}

tasks.register<JavaExec>("FetchGitlabVariables") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.ideplugins.ci_pipeline_lint.gitlab.FetchGitlabVariables")
    setArgsString(file("${projectDir}/src/main/resources/gitlab-variables.json").path)
}


dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.2")
        bundledPlugins(properties("platformBundledPlugins").map { it.split(',') })
        testBundledPlugins(properties("platformBundledPlugins").map { it.split(',')})
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
//        testFramework(TestFrameworkType.Plugin.Java)
    }

    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.sentrysdk) {
        exclude(group = "org.slf4j")
    }
    implementation(libs.annotations)
    testImplementation(libs.junit)
    testImplementation(libs.mockito)
    testImplementation(libs.mockwebserver) {
        exclude("junit")
    }
    testImplementation(libs.mockwebserverjunit5)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junitplatform)
    testRuntimeOnly(libs.junitengine)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

// Configure Gradle IntelliJ Plugin
intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        var changelog = file("build/docs/asciidoc/html/CHANGELOG.html")
        if (changelog.exists()){
            changeNotes = provider {
                Jsoup.parse(changelog)
                    .select("#releasenotes")[0].nextElementSibling()!!.children().subList(0, 10)
                    .joinToString("\n")
            }
        }
    }

    signing {
        val certChain = environment("JBM_CERTIFICATE_CHAIN").orNull
        val privateKey = environment("JBM_PRIVATE_KEY").orNull
        val password = environment("JBM_PRIVATE_KEY_PASSWORD").orNull

        if (certChain != null && privateKey != null && password != null) {
            certificateChainFile = file(certChain)
            privateKeyFile = file(privateKey)
            this.password = password
        }
    }

    publishing {
        token = environment("JBM_PUBLISH_TOKEN")
        channels.set(
            listOf(if ("true" == environment("PUSH_EAP").getOrElse("false")) "eap" else "default")
        )
    }
}



tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.compilerArgs = listOf("-Xlint:deprecation", "-Xlint:unchecked")
    }

    withType<Test> {
        useJUnitPlatform()
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
        finalizedBy("jacocoTestReport")
    }

    jacocoTestReport {
        classDirectories.setFrom(instrumentCode)
        reports {
            xml.required = true
        }
        finalizedBy("printCoverageForGitlab")
    }

    patchPluginXml {
        dependsOn("asciidoctorHtml")
    }

    runIde {
        autoReload = true
        outputs.cacheIf { false }
    }

}
