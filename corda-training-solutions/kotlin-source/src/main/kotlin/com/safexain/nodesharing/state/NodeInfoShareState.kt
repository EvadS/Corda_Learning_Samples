package com.safexain.nodesharing.state

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(com.safexain.nodesharing.contract.NodeShareContract::class)
data class NodeInfoShareState(
        val origin: Party,
        val target: Party,
        var nodeInfoContainer: com.safexain.nodesharing.model.NodeInfoContainer,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants: List<Party> get() = listOf(origin, target)
}



