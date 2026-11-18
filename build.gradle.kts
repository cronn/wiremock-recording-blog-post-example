plugins {
    id("org.springframework.boot") version "4.1.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.gradleup.shadow") version "9.6.1" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }
}
