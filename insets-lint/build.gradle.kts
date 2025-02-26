plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.insets.lint"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
}

dependencies {
    implementation(project(":insets-lint-check"))
    lintPublish(project(":insets-lint-check"))
}