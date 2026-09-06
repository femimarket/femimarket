import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.openapiGenerator)
}


kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
        // ffi cinterop (md_*/ae_*/db_*): header from build.sh's cbindgen step,
        // libffi.a linked straight from the per-triple cargo output dir.
        val cargoTriple = if (iosTarget.name == "iosArm64") "aarch64-apple-ios" else "aarch64-apple-ios-sim"
        iosTarget.compilations.getByName("main").cinterops.create("ffi") {
            defFile(project.file("src/nativeInterop/cinterop/ffi.def"))
            includeDirs(project.file("src/nativeInterop/cinterop"))
            extraOpts(
                "-staticLibrary", "libffi.a",
                "-libraryPath", rootProject.rootDir.resolve("ffi/target/$cargoTriple/release").absolutePath,
            )
        }
    }
    
    jvm()
    
    js {
        browser()
        useEsModules()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        useEsModules()
    }
    
    androidLibrary {
       namespace = "market.femi.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    val commonDeps = listOf(
//        compose.runtime,
//        compose.foundation,
//        compose.material3,
//        compose.ui,
//        compose.components.resources,
//        compose.components.uiToolingPreview,
//        libs.androidx.lifecycle.viewmodel,
//        libs.androidx.lifecycle.runtimeCompose,

        libs.ktor.client.core,
        libs.ktor.client.content.negotiation,
        libs.ktor.serialization.kotlinx.json,
        libs.ktor.client.websockets,
        libs.kotlinx.serialization.json,
        libs.ktor.client.logging,
        libs.ktor.client.auth,
        libs.ktor.client.core,
//        libs.navigation.compose,
        libs.kotlinx.io.core,
        compose.materialIconsExtended,
        libs.kotlinx.coroutinesCore,
        libs.multiplatform.settings.no.arg,
//        libs.supabase.bom,
//        libs.supabase.auth,
//        libs.supabase.postgrest,
    )

    
    sourceSets {
        commonMain {
            kotlin.srcDirs(
                layout.buildDirectory.dir("generated/source/elevenlabs/src/main/kotlin"),
                layout.buildDirectory.dir("generated/source/meta/src/main/kotlin"),
                layout.buildDirectory.dir("generated/source/care-service/src/commonMain/kotlin"),
                layout.buildDirectory.dir("generated/source/match-service/src/commonMain/kotlin"),
                layout.buildDirectory.dir("generated/source/ui-service/src/commonMain/kotlin"),
                layout.buildDirectory.dir("generated/source/matrix-service/src/commonMain/kotlin"),
                layout.buildDirectory.dir("generated/source/music-service/src/commonMain/kotlin"),
            )
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.documentfile)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            api(projects.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            commonDeps.forEach { implementation(it) }

            implementation(libs.openai.client)
            implementation(libs.kermit)
            implementation("io.github.shivathapaa:logger:1.4.0")
            implementation(libs.kotlinx.datetime)
            implementation(libs.jetbrains.navigation3.ui)
//            implementation(libs.composemediaplayer.audio)
            implementation("io.github.kdroidfilter:composemediaplayer:0.11.3")
            implementation("io.github.kdroidfilter:composemediaplayer-audio:0.11.3")

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.material.kolor)

            implementation(project.dependencies.platform(libs.trixnity.bom))
            implementation(libs.trixnity.clientserverapi.client)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        webMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.wrappers.browser)
            implementation(npm("mediabunny", "1.2.0"))
            implementation(npm("idb", "8.0.3"))
            implementation(npm("ffi", rootProject.rootDir.resolve("ffi/pkg")))
//            implementation(npm("rust", "/Users/u/femi.market/studio.femi.market/wasmApp"))
//            implementation(npm("wasm_xmp", "/Users/u/apps/femi/wasm-xmp/pkg"))
//            implementation(npm("wasm_id3", "/Users/u/apps/femi/wasm-id3/pkg"))
//            implementation(npm("wasm_infer", "/Users/u/apps/femi/wasm-infer/pkg"))
        }
        jsMain.dependencies {
//            implementation(libs.wrappers.browser)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        nativeMain.dependencies {
//            implementation(libs.native.driver)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
//            implementation(libs.sqlite.driver)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val metaServiceOpenapi by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("kotlin")

    // 1. inputSpec is the running meta-service's spec URL — a working server means good to go
    inputSpec.set("https://meta.femi.market/api-docs/openapi.json")

    // utoipa emits info.license without an identifier, which fails the generator's
    // spec validation — skip it (the error's own suggested switch)
    validateSpec.set(false)

    // 2. outputDir points exactly where the generated client lives
    outputDir.set("$buildDir/generated/source/meta")

    packageName.set("market.femi.meta")

    globalProperties.set(mapOf(
        "models" to "",
        "apis" to "",
        "supportingFiles" to "" // CRUCIAL: stops it from generating junk files next to main.kt
    ))

    configOptions.set(mapOf(
        "library" to "multiplatform",
        "dateLibrary" to "kotlinx-datetime",
        "useCoroutines" to "true",
        "omitGradleWrapper" to "true",
        "modelPropertyNaming" to "original"
    ))

    typeMappings.set(mapOf(
        "binary" to "kotlin.String",
        "UUID" to "String",
        "File" to "ByteArray"
    ))
}


val openApiGenerateElevenlabsForcedAlignment by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("kotlin")

    // 1. inputSpec MUST point to the .json file
    inputSpec.set("$rootDir/app/shared/src/commonMain/composeResources/files/elevenlabs-forced-alignment.json")

    // 2. outputDir points exactly where main.kt lives
    outputDir.set("$buildDir/generated/source/elevenlabs")

    packageName.set("com.example.elevenlabs")

    globalProperties.set(mapOf(
        "models" to "",
        "apis" to "",
        "supportingFiles" to "" // CRUCIAL: stops it from generating junk files next to main.kt
    ))

    configOptions.set(mapOf(
        "library" to "multiplatform",
        "dateLibrary" to "kotlinx-datetime",
        "useCoroutines" to "true",
        "omitGradleWrapper" to "true",
        "modelPropertyNaming" to "original"
    ))

    typeMappings.set(mapOf(
        "binary" to "kotlin.String",
        "UUID" to "String",
        "File" to "ByteArray"
    ))
}
