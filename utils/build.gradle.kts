plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.kotlinx.coroutines.android)
}

