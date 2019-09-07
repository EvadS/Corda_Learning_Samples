package com.safexain.cordapp.state

import java.time.Instant
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

data class NodeInfoContainerState(
        val target: Party,
        val origin: Party,
        var clientId: String = "",
        var fileName: String = "nodeInfo",
        var manipulationServiceNodeId: String = "",
        var lastUpdateDate: Instant = Instant.now(),
        var containerType : NodeInfoContainerType =  NodeInfoContainerType.NODE_INFO,
        var nameItem : String="",
        override val linearId: UniqueIdentifier

): LinearState{

    override val participants get() = listOf(target, origin)
}

enum class NodeInfoContainerType {
    NODE_INFO,
    UPDATE_CORDAPP
}
