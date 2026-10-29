plugins {
    id("perigee.android.app")
}

dependencies {
    implementation(project(":ui:shared"))
    implementation(libs.androidx.activity.compose)
}
