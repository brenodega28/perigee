plugins {
    id("perigee.desktop.app")
}

dependencies {
    implementation(project(":ui:shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "brenodega28.perigee.app.desktop.MainKt"
    }
}
