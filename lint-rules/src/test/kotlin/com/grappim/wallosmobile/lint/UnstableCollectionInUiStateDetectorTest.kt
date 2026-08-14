package com.grappim.wallosmobile.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.grappim.wallosmobile.lint.UnstableCollectionInUiStateDetector.Companion.ISSUE_UNSTABLE_COLLECTION
import org.junit.Test

class UnstableCollectionInUiStateDetectorTest {

    @Test
    fun whenUiStateHasAListParamThenDetectedAsAViolation() {
        val callSite = """
            package test

            data class FooUiState(
                val items: List<String> = emptyList()
            )
        """

        assertLintError(listOf(kotlin(callSite).indented(), composableStub, immutableListStub))
    }

    @Test
    fun whenUiStateHasAnImmutableListParamThenNotDetectedAsAViolation() {
        val callSite = """
            package test

            import kotlinx.collections.immutable.ImmutableList
            import kotlinx.collections.immutable.persistentListOf

            data class FooUiState(
                val items: ImmutableList<String> = persistentListOf()
            )
        """

        assertNoLintError(listOf(kotlin(callSite).indented(), composableStub, immutableListStub))
    }

    @Test
    fun whenPublicComposableHasAListParamThenDetectedAsAViolation() {
        val callSite = """
            package test

            import androidx.compose.runtime.Composable

            @Composable
            fun FooContent(items: List<String>) {}
        """

        assertLintError(listOf(kotlin(callSite).indented(), composableStub, immutableListStub))
    }

    @Test
    fun whenPrivateComposableHasAListParamThenNotDetectedAsAViolation() {
        val callSite = """
            package test

            import androidx.compose.runtime.Composable

            @Composable
            private fun FooContent(items: List<String>) {}
        """

        assertNoLintError(listOf(kotlin(callSite).indented(), composableStub, immutableListStub))
    }

    private val composableStub = kotlin(
        """
            package androidx.compose.runtime

            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
            annotation class Composable
        """
    ).indented()

    private val immutableListStub = kotlin(
        """
            package kotlinx.collections.immutable

            interface ImmutableList<out E> : List<E>

            fun <E> persistentListOf(): ImmutableList<E> = object : ImmutableList<E>, List<E> by emptyList() {}
        """
    ).indented()

    private fun assertLintError(files: List<TestFile>) {
        lint()
            .files(*files.toTypedArray())
            .issues(ISSUE_UNSTABLE_COLLECTION)
            .allowMissingSdk()
            .run()
            .expectContains("[UnstableCollectionInUiState]")
    }

    private fun assertNoLintError(files: List<TestFile>) {
        lint()
            .files(*files.toTypedArray())
            .issues(ISSUE_UNSTABLE_COLLECTION)
            .allowMissingSdk()
            .run()
            .expectClean()
    }
}
