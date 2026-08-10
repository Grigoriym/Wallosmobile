import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.grappim.wallosmobile.buildlogic.AppBuildTypes
import com.grappim.wallosmobile.buildlogic.AppFlavors
import com.grappim.wallosmobile.buildlogic.configureFlavors
import com.grappim.wallosmobile.buildlogic.configureKotlinAndroid
import com.grappim.wallosmobile.buildlogic.configureLinting
import com.grappim.wallosmobile.buildlogic.configureTests
import com.grappim.wallosmobile.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "io.insert-koin.compiler.plugin")

            extensions.configure<ApplicationExtension> {
                defaultConfig.apply {
                    targetSdk = libs.findVersion("targetSdk").get().toString().toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                // One release identity per store flavor — assigned on the flavor below, not on
                // the `release` build type, so `debug` keeps AGP's own default debug signing.
                signingConfigs {
                    AppFlavors.entries.forEach { flavor ->
                        create("${flavor.title}Release") {
                            val envSuffix = flavor.title.uppercase()
                            storeFile = rootProject.file("wallosmobile_keystore_${flavor.title}_release.jks")
                            storePassword = System.getenv("WALLOS_STORE_PASS_$envSuffix")
                            keyAlias = System.getenv("WALLOS_ALIAS_$envSuffix")
                            keyPassword = System.getenv("WALLOS_KEY_PASS_$envSuffix")
                            enableV2Signing = true
                            enableV3Signing = true
                        }
                    }
                }

                buildTypes {
                    debug {
                        applicationIdSuffix = AppBuildTypes.DEBUG.applicationIdSuffix

                        isDebuggable = true
                        isMinifyEnabled = false
                        isShrinkResources = false
                    }
                    release {
                        applicationIdSuffix = AppBuildTypes.RELEASE.applicationIdSuffix

                        isDebuggable = false
                        isMinifyEnabled = true
                        isShrinkResources = true

                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                bundle {
                    language {
                        enableSplit = false
                    }
                }

                packaging.resources.excludes.apply {
                    add("META-INF/ASL2.0")
                    add("META-INF/notice.txt")
                    add("META-INF/NOTICE.txt")
                    add("META-INF/NOTICE")
                    add("META-INF/license.txt")
                    add("DEPENDENCIES")
                }

                buildFeatures.apply {
                    compose = true
                }

                configureFlavors(this) { flavor ->
                    if (this is ApplicationProductFlavor) {
                        signingConfig = signingConfigs.getByName("${flavor.title}Release")
                    }
                }
                configureKotlinAndroid(this)
            }

            // `:androidApp` holds MainActivity and the Koin startup glue — real Kotlin that
            // the gates have to cover, even though it is not a KMP module.
            configureTests()
            configureLinting()
        }
    }
}
