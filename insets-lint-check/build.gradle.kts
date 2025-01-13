plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    // id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    val lintVersion = "30.0.0"
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    compileOnly("com.android.tools.lint:lint-api:${lintVersion}")
    compileOnly("com.android.tools.lint:lint-checks:$lintVersion")
    testImplementation("com.android.tools.lint:lint:$lintVersion")
    testImplementation("com.android.tools.lint:lint-tests:$lintVersion")
    testImplementation("junit:junit:4.13.2")
}