rootProject.name = "yaml-pipeline-lint"
pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }
}
plugins {
    id("com.gradle.develocity") version("3.18")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}


buildCache {
    local {
    }
    remote<HttpBuildCache> {
        url = uri( System.getenv("GRADLE_CACHE_SERVER"))
        setAllowInsecureProtocol(!System.getenv("CI").isNullOrEmpty())
        isPush = true
        credentials {
            username = System.getenv("GRADLE_CACHE_USERNAME")
            password = System.getenv("GRADLE_CACHE_PASSWORD")
        }
    }
}
