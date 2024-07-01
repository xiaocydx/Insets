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
    implementation(PublishLibs.`androidx-core`)
    implementation(PublishLibs.`androidx-appcompat`)
    testImplementation(PublishLibs.`androidx-viewpager2`)
    implementation(PublishLibs.`androidx-fragment-new`)
    testImplementation(PublishLibs.`androidx-test-core`)
    testImplementation(PublishLibs.truth)
    testImplementation(PublishLibs.robolectric)
    testImplementation(PublishLibs.mockk)
    testImplementation(PublishLibs.junit)
}