package net.corda.training.state

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.training.contract.NodeInfoContract

@BelongsToContract(NodeInfoContract::class)
data class NodeInfoShareState(
        val origin: Party,
        val target: Party,
        var nodeInfo: NodeInfoShare,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants: List<Party> get() = listOf(origin, target)
}

@CordaSerializable
data class NodeInfoShare(
        val message: String,
        val test: String
)