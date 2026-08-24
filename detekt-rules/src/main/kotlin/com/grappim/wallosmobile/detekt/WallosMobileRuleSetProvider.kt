package com.grappim.wallosmobile.detekt

import dev.detekt.api.Config
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class WallosMobileRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("WallosMobile")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        mapOf(
            RuleName("UnstableCollectionInUiState") to { config: Config ->
                UnstableCollectionInUiStateRule(config)
            }
        )
    )
}
