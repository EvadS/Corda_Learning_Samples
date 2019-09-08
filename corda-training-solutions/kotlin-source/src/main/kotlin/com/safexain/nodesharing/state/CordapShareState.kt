package com.safexain.nodesharing.state

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(com.safexain.nodesharing.contract.NodeShareContract::class)
class CordapShareState (
        val origin: Party,
        val target: Party,
        var cordapShareContainer: com.safexain.nodesharing.model.CordappShareContainer,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants: List<Party> get() = listOf(origin, target)
}