import com.grappim.wallosmobile.buildlogic.configureComposeStabilityMarker
import com.grappim.wallosmobile.buildlogic.configureComposeStabilityReports
import org.gradle.api.Plugin
import org.gradle.api.Project

// Applied alongside `wallosmobile.kmp.library` on `*/domain` modules whose types are consumed as
// Composable parameters elsewhere — see docs/compose/stability-reports.md.
class KmpLibraryStabilityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureComposeStabilityMarker()
            configureComposeStabilityReports()
        }
    }
}
