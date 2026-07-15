plugins {
    kotlin("multiplatform") version "1.9.24" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
    id("com.android.application") version "8.4.1" apply false
    id("org.jetbrains.compose") version "1.6.11" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
    buildDir = file("/tmp/gradle-build/${project.name}")
}
