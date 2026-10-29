import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "perigee"
            packageVersion = "1.0.0"
        }
    }
}
