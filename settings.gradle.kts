pluginManagement {
	repositories {
        gradlePluginPortal()
        mavenCentral()
		maven("https://maven.fabricmc.net") {
			name = "FabricMC"
		}

	}
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Cammies-Minecart-Tweaks"
