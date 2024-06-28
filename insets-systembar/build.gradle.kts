import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.insets.systembar"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
    configurations {
        testImplementation.extendsFrom(compileOnly)
        androidTestImplementation.extendsFrom(compileOnly)
    }
}

dependencies {
    compileOnly(project(":insets"))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.fragment:fragment:1.3.6")
    implementation(Libs.`androidx-core`)
    testImplementation(Libs.`androidx-viewpager2`)
    testImplementation(Libs.truth)
    testImplementation(Libs.robolectric)
    testImplementation(Libs.mockk)
    testImplementation(Libs.`androidx-test-core`)
    testImplementation(Libs.junit)
}