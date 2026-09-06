import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.openapiGenerator)
}

kotlin {
    js {
        browser()
        binaries.executable()
        useEsModules()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
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
            )
        }
        commonMain.dependencies {
            implementation(projects.app.shared)

            implementation(libs.compose.ui)



            //!
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)


            commonDeps.forEach { implementation(it) }


            implementation(libs.openai.client)
            implementation("io.github.shivathapaa:logger:1.4.0")
            implementation(libs.kotlinx.datetime)

            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

        }
    }
}

