plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xiaocydx.insets.sample"
    defaultConfig { applicationId = "com.xiaocydx.insets.sample" }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":insets"))
    implementation(project(":insets-compat"))
    implementation(project(":insets-systembar"))
    implementation(Libs.cxrv)
    implementation(Libs.`cxrv-binding`)
    implementation(Libs.`androidx-core-ktx`)
    implementation(Libs.`androidx-appcompat`)
    implementation(Libs.`androidx-transition`)
    implementation(Libs.`androidx-constraintlayout`)
    implementation(Libs.material)
}