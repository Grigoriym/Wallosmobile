import com.grappim.wallosmobile.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpNetworkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.findLibrary("ktor.core").get())
                    }
                    // Android is the only target for now; the Darwin and CIO engines go here
                    // when configureKmp() gains those targets.
                    androidMain.dependencies {
                        implementation(libs.findLibrary("ktor.client.okhttp").get())
                    }
                }
            }
        }
    }
}
