import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "de.luca.dungeon_master_manager.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "de.luca.dungeon_master_manager"
            packageVersion = "1.0.0"

            val iconsDir = project.file("src/main/resources/icons")
            val pngIcon = iconsDir.resolve("app_icon.png")
            val icoIcon = iconsDir.resolve("app_icon.ico")
            val icnsIcon = iconsDir.resolve("app_icon.icns")

            windows {
                menuGroup = "Dungeon Master Manager"
                if (icoIcon.exists()) {
                    iconFile.set(icoIcon)
                } else if (pngIcon.exists()) {
                    iconFile.set(pngIcon)
                }
            }
            linux {
                if (pngIcon.exists()) {
                    iconFile.set(pngIcon)
                }
            }
            macOS {
                if (icnsIcon.exists()) {
                    iconFile.set(icnsIcon)
                } else if (pngIcon.exists()) {
                    iconFile.set(pngIcon)
                }
            }
        }
    }
}