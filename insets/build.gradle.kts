plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.insets"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
}

dependencies {
    implementation(Libs.`androidx-core`)
}