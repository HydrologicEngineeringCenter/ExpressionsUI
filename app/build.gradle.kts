import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure

plugins {
    java
    application
    `maven-publish`
    id("com.palantir.git-version") version "3.0.0"
}

group = "mil.army.usace.hec"

repositories {
    mavenCentral()
    maven {
        url = uri("https://www.hec.usace.army.mil/nexus/repository/maven-releases/")
    }
}

dependencies {
    implementation("mil.army.usace.hec:expressions:1.0.22")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("expression.builder.App")
}

tasks.test {
    useJUnitPlatform()
}

// A release is published by tagging main with "vX.Y" (e.g. v1.1). The leading "v" is
// stripped so the artifact version is just the number (e.g. 1.1). The release version is
// used ONLY when HEAD is exactly on the tagged commit with a clean tree (isCleanTag); any
// commit past the tag falls back to -SNAPSHOT, mirroring Expressions' versioning scheme.
fun versionLabel(gitInfo: VersionDetails): String {
    val tag = gitInfo.lastTag?.trim()
    println("lastTag=$tag isCleanTag=${gitInfo.isCleanTag} branchName=${gitInfo.branchName} gitHashFull=${gitInfo.gitHashFull}")
    if (gitInfo.isCleanTag && !tag.isNullOrEmpty() && Regex("v\\d+(\\.\\d+)*").matches(tag)) {
        return tag.substring(1)
    }
    return "-SNAPSHOT"
}

val versionDetails: Closure<VersionDetails> by extra
version = versionLabel(versionDetails.call())
println("selected tag: $version")

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "mil.army.usace.hec"
            artifactId = "expressionsui"
        }
    }
    repositories {
        maven {
            name = "hecNexus"
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
            val releasesRepoUrl = "https://www.hec.usace.army.mil/nexus/repository/maven-releases/"
            val snapshotsRepoUrl = "https://www.hec.usace.army.mil/nexus/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
}

tasks.publish {
    dependsOn(tasks.build)
}

tasks.build {
    dependsOn(tasks.jar)
}