package com.grappim.wallosmobile.detekt

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnstableCollectionInUiStateRuleTest {

    private val rule = UnstableCollectionInUiStateRule(TestConfig())

    @Test
    fun whenUiStateHasAListParamThenDetectedAsAViolation() {
        val code = """
            package test

            data class FooUiState(
                val items: List<String> = emptyList()
            )
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun whenUiStateHasAnImmutableListParamThenNotDetectedAsAViolation() {
        val code = """
            package test

            import kotlinx.collections.immutable.ImmutableList
            import kotlinx.collections.immutable.persistentListOf

            data class FooUiState(
                val items: ImmutableList<String> = persistentListOf()
            )
        """.trimIndent()

        val findings = rule.lint(code)

        assertTrue(findings.isEmpty())
    }

    @Test
    fun whenPublicComposableHasAListParamThenDetectedAsAViolation() {
        val code = """
            package test

            import androidx.compose.runtime.Composable

            @Composable
            fun FooContent(items: List<String>) {}
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun whenPrivateComposableHasAListParamThenNotDetectedAsAViolation() {
        val code = """
            package test

            import androidx.compose.runtime.Composable

            @Composable
            private fun FooContent(items: List<String>) {}
        """.trimIndent()

        val findings = rule.lint(code)

        assertTrue(findings.isEmpty())
    }
}
