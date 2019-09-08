package net.corda.training.state

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.training.contract.NodeInfoContract
import net.corda.training.model.CordappShareContainer
import net.corda.training.model.NodeInfoContainer

@BelongsToContract(NodeInfoContract::class)
class CordapShareState (
        val origin: Party,
        val target: Party,
        var cordapShareContainer: CordappShareContainer,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants: List<Party> get() = listOf(origin, target)
}