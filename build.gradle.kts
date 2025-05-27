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
    alias(libs.plugins.jacocolog)
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

dependencies {
    intellijPlatform {
        create(properties("platformType"), properties("platformVersion"), useInstaller = false)
        bundledPlugins(properties("platformBundledPlugins").map { it.split(',') })
//        plugins(properties("platformPlugins").map { it.split(',') })
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
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
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junitplatform)
    testRuntimeOnly(libs.junitengine)
}

// Configure Gradle IntelliJ Plugin
intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        changeNotes = provider {
            Jsoup.parse(file("build/docs/changelog.html"))
                .select("#releasenotes")[0].nextElementSibling()?.children()
                ?.toString()
        }
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
        }
    }

    signing {
        certificateChainFile = file(environment("JBM_CERTIFICATE_CHAIN"))
        privateKeyFile = file(environment("JBM_PRIVATE_KEY"))
        password = environment("JBM_PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("JBM_PUBLISH_TOKEN")
        channels.set(
            listOf(if ("true" == environment("PUSH_EAP").getOrElse("false")) "eap" else "default")
        )
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

}

val runIdeForManualTests by intellijPlatformTesting.runIde.registering {
    prepareSandboxTask {
        sandboxDirectory = project.layout.buildDirectory.dir("custom-sandbox")
        sandboxSuffix = ""
    }

    task {
        doFirst {
            copy {
                from("${projectDir}/src/test/resources/ide/options/")
                into(project.layout.buildDirectory.dir("custom-sandbox/config/options"))
                include("*.xml")
            }
            copy {
                from("${projectDir}/src/test/resources/ide/options/inspectionProfiles")
                into(project.layout.buildDirectory.dir("custom-sandbox/config/options/inspectionProfiles"))
            }
        }
        systemProperty("idea.auto.reload.plugins", "false")
        systemProperty("idea.trust.all.projects", "true")
        systemProperty("ide.show.tips.on.startup.default.value", "false")
        systemProperty("idea.is.internal", "true")
        systemProperty("idea.disposer.debug", "on")
        systemProperty("nosplash", "true")
        args = listOf("${projectDir}/src/test/resources/annotator/")
    }
}

val runIdeEAP by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.IntellijIdeaCommunity
    version = "252-EAP-SNAPSHOT"
}

tasks.register<JavaExec>("FetchGitlabVariables") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.ideplugins.ci_pipeline_lint.gitlab.FetchGitlabVariables")
    setArgsString(file("${projectDir}/src/main/resources/gitlab-variables.json").path)
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

    init {
        version = semver.version
    }

    asciidoctor {
        setSourceDir(baseDir)
        sources {
            include("changelog.adoc")
        }
        setOutputDir(file("build/docs"))
    }

    jacocoTestReport {
        classDirectories.setFrom(instrumentCode)
        reports {
            xml.required = true
        }
    }

    patchPluginXml {
        dependsOn(asciidoctor)
    }

    intellijPlatformTesting {
        runIde {
            register("runIdeForUiTests") {
                task {
                    jvmArgumentProviders += CommandLineArgumentProvider {
                        listOf(
                            "-Drobot-server.port=8082",
                            "-Dide.mac.message.dialogs.as.sheets=false",
                            "-Djb.privacy.policy.text=<!--999.999-->",
                            "-Djb.consents.confirmation.enabled=false",
                        )
                    }
                }

                plugins {
                    robotServerPlugin()
                }
            }
        }
    }
}
