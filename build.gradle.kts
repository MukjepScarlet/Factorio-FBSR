plugins {
    java
    kotlin("jvm") version "2.1.10"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
//    implementation(kotlin("stdlib"))
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("com.github.demodude4u:Java-Factorio-Data-Wrapper:575b5cb4b0")
    implementation("com.github.MukjepScarlet:Discord-Core-Bot-Apple:0ec17f1d5f")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

kotlin {
    jvmToolchain(11)

    compilerOptions.freeCompilerArgs.add("-Xjsr305=strict")
}
