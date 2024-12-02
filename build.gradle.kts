import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    idea
    `java-library`
    java
    distribution
    kotlin("jvm") version "1.5.31" apply false
}

group = "example.domain"
version = "1.0.0.4"


allprojects {
    repositories {

        mavenLocal()

        // 一直403,根本没法用
        // maven {
        //     isAllowInsecureProtocol = true
        //     setUrl("http://mvnrepository.com/")
        // }

        // public
        maven {
            isAllowInsecureProtocol = true
            setUrl("https://maven.aliyun.com/repository/public")
        }


        maven {
            isAllowInsecureProtocol = true
            setUrl("http://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }

        // gradle-plugin
        maven {
            setUrl("https://maven.aliyun.com/repository/gradle-plugin")
        }
        // central
        maven {
            setUrl("https://maven.aliyun.com/repository/central")
        }
        // jcenter
        maven {
            setUrl("https://maven.aliyun.com/repository/jcenter")
        }

        maven {
            isAllowInsecureProtocol = true
            setUrl("http://repo1.maven.org/maven2/")
        }

        maven {
            isAllowInsecureProtocol = true
            setUrl("http://repo2.maven.org/maven2/")
        }

        mavenCentral()
    }

    apply(plugin = "java")
    tasks.compileJava {
        options.javaModuleVersion.set(provider { project.version as String })
        options.release.set(11)
        options.encoding = "UTF-8"
    }
}

dependencies {
    // logback-core
    implementation("ch.qos.logback:logback-classic:1.2.10") {
        exclude("org.slf4j")
    }

    implementation("org.slf4j:slf4j-api:1.7.32")

    implementation("org.apache.curator:curator-recipes:5.1.0") { // // high-level
        exclude("log4j")
        exclude("org.slf4j")
    }

    implementation("cn.hutool", "hutool-all", "5.6.3")
}


tasks {
    compileJava {
        options.isIncremental = true
        options.isFork = true
        options.isFailOnError = true
    }
    jar {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "manager.Application",
                    "Author" to "-",
                    "BuildTime" to "${System.currentTimeMillis()}"
                )
            )
        }
    }
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("zkview")
    destinationDirectory.set(File("build"))
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        {
            configurations.runtimeClasspath.get().filter {
                it.name.endsWith(".jar")
            }.map { zipTree(it) }
        }
    ).exclude("META-INF/*")

    val nowOffsetDateTime = OffsetDateTime.ofInstant(
        Instant.ofEpochSecond(Instant.now().epochSecond),
        ZoneId.systemDefault()
    )
    manifest {
        attributes(
            mapOf(
                "Version" to archiveVersion.get(),
                "Main-Class" to "manager.Application",
                "Author" to "-",
                "Package" to "Gradle",
                "BuildTime" to DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(nowOffsetDateTime)
            )
        )
    }
}
