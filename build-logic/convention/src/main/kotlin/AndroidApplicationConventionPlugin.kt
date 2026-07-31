import com.android.build.api.dsl.ApplicationExtension
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

                buildTypes {
                    debug {
                        isDebuggable = true
                        isMinifyEnabled = false
                        isShrinkResources = false
                    }
                    release {
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

                configureKotlinAndroid(this)
            }

            // `:androidApp` holds MainActivity and the Koin startup glue — real Kotlin that
            // the gates have to cover, even though it is not a KMP module.
            configureTests()
            configureLinting()
        }
    }
}
