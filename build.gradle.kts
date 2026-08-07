@file:Suppress("UnstableApiUsage")

import net.fabricmc.loom.task.LoomTasks
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    idea
    `maven-publish`
    alias(libs.plugins.fabric.loom.remap)
    alias(libs.plugins.idea.ext)
}

val tag = providers.environmentVariable("TAG")
val isPreviewBuild = tag.orNull?.matches(Regex(".+-.+")) ?: false
val buildNumber = tag.orElse(providers.environmentVariable("BUILD_NUMBER").map { "build.${it}" })

version = tag.orElse(provider { buildString {
    append("0.1.0-development")
    if(isPreviewBuild && !tag.isPresent) {
        append(buildNumber.map { "+${it}" }.getOrElse(""))
    }
} }).get()

println("Building ${project.name} $version")

val modID: String = providers.gradleProperty("mod_id").get()

repositories {
    exclusiveContent {
        forRepository {
            maven("https://maven.covers1624.net") {
                name = "Covers1624"
            }
        }
        filter {
            includeGroup("net.covers1624")
        }
    }

    // FIXME currently unavailable, using backup
    // maven("https://maven.terraformersmc.com/releases") {
    maven("https://maven.gnomecraft.net/releases") {
		name = "TerraformersMC"
	}
	maven("https://maven.teamresourceful.com/repository/maven-public")
    maven("https://maven.uuid.gg/releases")
	maven("https://maven.blamejared.com")
}

dependencies {
    localRuntime(libs.devlogin)
	minecraft(libs.minecraft)
	mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.minecraft.get()}:${libs.versions.parchment.mappings.get()}@zip")
    })

    compileOnly(libs.jspecify)
    compileOnly(libs.jetbrains.annotations)

    compileOnly(libs.autoservice.annotations)
    annotationProcessor(libs.autoservice)

	modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    modImplementation(libs.commonnetwork.fabric)

    modCompileOnly(libs.modmenu.fabric) {
        isTransitive = false
    }
    modLocalRuntime(libs.modmenu.fabric) {
        isTransitive = false
    }

	modImplementation(libs.resourcefulconfig.fabric) {
        isTransitive = false
    }

    modImplementation(libs.sparkweave.fabric)
}

loom {
    accessWidenerPath = file("src/main/resources/${modID}.classtweaker")

    mods {
        create(modID) {
            sourceSet(sourceSets["main"])
        }
    }

    runs {
        named("client") {
            client()
            programArguments.addAll("--launch_target", "net.fabricmc.loader.impl.launch.knot.KnotClient")
            mainClass = "net.covers1624.devlogin.DevLogin"
            displayName = "Fabric Client"
            runDirectory = layout.projectDirectory.dir("run/client")
        }

        named("server") {
            server()
            displayName = "Fabric Server"
            runDirectory = layout.projectDirectory.dir("run/server")
        }

        configureEach {
            appendProjectPathToDisplayName = false
            systemProperties.put("sparkweave.debug", "true")
            systemProperties.put("mixin.debug", "true")

            // register as Gradle runs instead of IDEA runs
            // https://github.com/FabricMC/fabric-loom/issues/1349
            generateRunConfig = false
            rootProject.idea.project.settings.runConfigurations.create<org.jetbrains.gradle.ext.Gradle>(displayName.get()) {
                taskNames = listOf(LoomTasks.getRunConfigTaskName(this@configureEach))
                setProject(project)
            }
        }
    }
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
		vendor = JvmVendorSpec.MICROSOFT
	}

    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = libs.versions.java.get().toInt()
    options.compilerArgs.add("-Xlint:unchecked")
}

tasks.named<Jar>("jar") {
    from("LICENSE.md") {
        rename("LICENSE.md", "LICENSE_{modID}.md")
    }

    manifest.attributes(mapOf<String, Any>(
        "Specification-Title" to rootProject.name,
        "Specification-Vendor" to "Cammie",
        "Specification-Version" to archiveVersion,

        "Implementation-Title" to "{project.name}-Fabric",
        "Implementation-Vendor" to "Up",
        "Implementation-Version" to archiveVersion,

        "Built-On-Java" to "${providers.systemProperty("java.vm.version").orNull} (${providers.systemProperty("java.vm.vendor").orNull})",
        "Built-On-Minecraft" to libs.versions.minecraft.get()
    ))
}

tasks.named<Jar>("sourcesJar") {
    from("LICENSE.md") {
        rename("LICENSE.md", "LICENSE_{modID}.md")
    }
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = "UTF-8"
}

tasks.named<ProcessResources>("processResources") {
    val expandProps = mapOf(
        "version" to project.version,
        "maven_group_id" to project.group,
        "mod_id" to modID,
        "mod_display_name" to providers.gradleProperty("mod_display_name").get(),
        "mod_description" to providers.gradleProperty("mod_description").get(),
        "sources_url" to providers.gradleProperty("sources_url").get(),
        "issues_url" to providers.gradleProperty("issues_url").get(),
        "license_url" to providers.gradleProperty("license_url").get(),
        "discord_url" to providers.gradleProperty("discord_url").get(),
        "homepage_url" to providers.gradleProperty("homepage_url").get(),
        "curseforge_id" to providers.gradleProperty("curseforge_id").get(),
        "modrinth_id" to providers.gradleProperty("modrinth_id").get(),

        "java_version" to libs.versions.java.get(),
        "minecraft_version" to libs.versions.minecraft.get(),
        "fabric_loader_version" to libs.versions.fabric.loader.get(),
    )

    filesMatching("META-INF/*mods.toml") {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "*.mod.json", "*.mixins.json")) {
        expand(expandProps.mapValues { it.value.toString().replace("\n", "\\n") })
    }

    inputs.properties(expandProps)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "${rootProject.name}-Fabric"
            from(components["java"])
        }
    }

    providers.environmentVariable("MAVEN_UPLOAD_URL").orNull?.let { url ->
        repositories {
            maven(uri(url)) {
                credentials {
                    username = providers.environmentVariable("MAVEN_UPLOAD_USERNAME").orNull
                    password = providers.environmentVariable("MAVEN_UPLOAD_PASSWORD").orNull
                }
            }
        }
    }
}

// IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
idea {
    module {
        isDownloadSources = true
    }
}
