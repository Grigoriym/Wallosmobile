import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.grappim.wallosmobile.buildlogic.configureKmp
import com.grappim.wallosmobile.buildlogic.configureLinting
import com.grappim.wallosmobile.buildlogic.configureTests
import com.grappim.wallosmobile.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // KMP must be applied first so the android plugin can hook into it
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
            }

            // The android DSL lives as a sub-extension of the kotlin extension
            val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()
            (kotlinExt as ExtensionAware).extensions
                .configure<KotlinMultiplatformAndroidLibraryExtension> {
                    compileSdk = libs.findVersion("compileSdk").get().toString().toInt()
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                    namespace = "com.grappim.wallosmobile" + path.replace(':', '.').replace("-", "")

                    // `com.android.kotlin.multiplatform.library` creates no host-test
                    // compilation unless asked. Without this, `commonTest` belongs to no
                    // compilation, the test dependencies from `configureTests()` are inert,
                    // and there is no test task to run. The reference projects don't need it
                    // because they get their test task from a `jvm()` target — WallosMobile
                    // is Android-only, so `testDebugUnitTest` is it.
                    withHostTestBuilder {}.configure {
                        isReturnDefaultValues = true
                        isIncludeAndroidResources = true
                    }
                }

            configureKmp()
            configureTests()
            configureLinting()
        }
    }
}
