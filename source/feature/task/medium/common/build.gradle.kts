plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.feature.task.medium.common"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:util"))
    implementation(project(":core:datastore"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)
}