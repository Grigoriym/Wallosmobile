package com.grappim.wallosmobile.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiModifier
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUMethod
import java.util.EnumSet

/**
 * CLAUDE.md's Compose rules: "`ImmutableList` / `persistentListOf()` over `List` in state classes
 * and Composable params, for stable recomposition." Nothing failed a build on a violation before
 * this detector — M20's stability-report tooling can surface one, but it's an opt-in report, not
 * a gate.
 *
 * A `List<T>`/`MutableList<T>` parameter erases to the same `java.util.List` PSI-resolved class
 * either way (Kotlin's `List`/`MutableList` are both backed by `java.util.List` at the JVM level,
 * distinguished only at the Kotlin type-system level), so a single qualified-name check against
 * the *declared* type's resolved class covers both — and does not false-positive on
 * `kotlinx.collections.immutable.ImmutableList`, which resolves to its own distinct interface even
 * though it structurally implements `List`.
 */
class UnstableCollectionInUiStateDetector :
    Detector(),
    SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UParameter::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitParameter(node: UParameter) {
            if (!isPlainListType(node.type as? PsiClassType)) return

            val containingMethod = node.getContainingUMethod() ?: return
            val containingClass = node.getContainingUClass()

            val inUiStateConstructor = containingMethod.isConstructor &&
                containingClass?.name?.endsWith("UiState") == true

            val inPublicComposable = containingMethod.findAnnotation(COMPOSABLE_ANNOTATION) != null &&
                !containingMethod.hasModifierProperty(PsiModifier.PRIVATE)

            if (inUiStateConstructor || inPublicComposable) {
                context.report(
                    ISSUE_UNSTABLE_COLLECTION,
                    node as UElement,
                    context.getLocation(node as UElement),
                    "`${node.name}` is a `List`, which is unstable for Compose recomposition. " +
                        "Use `ImmutableList`/`persistentListOf()` instead."
                )
            }
        }
    }

    private fun isPlainListType(type: PsiClassType?): Boolean {
        val resolved = type?.resolve() ?: return false
        return resolved.qualifiedName == "java.util.List"
    }

    companion object {
        private const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"

        val ISSUE_UNSTABLE_COLLECTION: Issue = Issue.create(
            id = "UnstableCollectionInUiState",
            briefDescription = "List used where ImmutableList is expected",
            explanation = "A plain `List`/`MutableList` in a `*UiState` class or a public " +
                "`@Composable` parameter makes Compose treat it as unstable, forcing extra " +
                "recomposition. Use `ImmutableList`/`persistentListOf()` instead.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                UnstableCollectionInUiStateDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
