plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xiaocydx.insets.sample"
    defaultConfig { applicationId = "com.xiaocydx.insets.sample" }
    kotlinOptions { jvmTarget = Versions.jvmTarget }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":insets"))
    implementation(project(":insets-compat"))
    implementation(project(":insets-systembar"))
    implementation(project(":insets-lint"))
    implementation(CommonLibs.cxrv)
    implementation(CommonLibs.`cxrv-binding`)
    implementation(CommonLibs.`androidx-core-ktx`)
    implementation(CommonLibs.`androidx-appcompat`)
    implementation(CommonLibs.`androidx-transition`)
    implementation(CommonLibs.`androidx-constraintlayout`)
    implementation(CommonLibs.material)
}