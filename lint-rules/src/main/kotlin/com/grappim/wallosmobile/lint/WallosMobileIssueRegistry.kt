package com.grappim.wallosmobile.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class WallosMobileIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(
            UnstableCollectionInUiStateDetector.ISSUE_UNSTABLE_COLLECTION
        )

    override val api: Int
        get() = CURRENT_API

    override val vendor = Vendor(
        vendorName = "WallosMobile",
        identifier = "com.grappim.wallosmobile:lint-rules",
        feedbackUrl = "https://github.com/Grigoriym/Wallosmobile/issues"
    )
}
