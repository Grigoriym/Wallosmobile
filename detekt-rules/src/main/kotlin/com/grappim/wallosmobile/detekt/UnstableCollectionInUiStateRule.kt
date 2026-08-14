package com.grappim.wallosmobile.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtUserType
import java.net.URI

/**
 * detekt port of `:lint-rules`' `UnstableCollectionInUiStateDetector` (M25), closing
 * `docs/revisit.md` #1 — a Lint `lintChecks` detector only ever sees the module that declares it,
 * so it never reached the `feature:*:ui`/`composeApp`/`uikit` code the check exists for.
 *
 * A text check against the type reference's simple name, not a resolved-class check: `List`/
 * `MutableList` are never spelled that way once aliased to `kotlinx.collections.immutable
 * .ImmutableList`, so there's no erasure collision to guard against the way UAST's PSI resolution
 * needed for (25.1's `Note:` point 1).
 */
class UnstableCollectionInUiStateRule(config: Config) :
    Rule(
        config,
        "A plain `List`/`MutableList` in a `*UiState` class or a public `@Composable` parameter " +
            "makes Compose treat it as unstable, forcing extra recomposition. Use " +
            "`ImmutableList`/`persistentListOf()` instead.",
        URI("https://developer.android.com/develop/ui/compose/performance/stability")
    ) {

    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)

        if (!isPlainListType(parameter)) return

        val ownerFunction = parameter.ownerFunction

        val inUiStateConstructor = ownerFunction is KtPrimaryConstructor &&
            ownerFunction.getContainingClassOrObject().name?.endsWith("UiState") == true

        val inPublicComposable = ownerFunction is KtNamedFunction &&
            ownerFunction.annotationEntries.any { it.shortName?.asString() == "Composable" } &&
            !ownerFunction.hasModifier(KtTokens.PRIVATE_KEYWORD)

        if (inUiStateConstructor || inPublicComposable) {
            report(
                Finding(
                    entity = Entity.atName(parameter),
                    message = "`${parameter.name}` is a `List`, which is unstable for Compose " +
                        "recomposition. Use `ImmutableList`/`persistentListOf()` instead."
                )
            )
        }
    }

    private fun isPlainListType(parameter: KtParameter): Boolean {
        val simpleName = (parameter.typeReference?.typeElement as? KtUserType)?.referencedName
        return simpleName == "List" || simpleName == "MutableList"
    }
}
