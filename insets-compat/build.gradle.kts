plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.insets.compat"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
}

dependencies {
    compileOnly(project(":insets"))
    implementation(PublishLibs.`androidx-core`)
    implementation(PublishLibs.hiddenapibypass)
}