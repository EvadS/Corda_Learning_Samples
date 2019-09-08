package com.safexain.nodesharing.model

import net.corda.core.serialization.CordaSerializable
import java.time.Instant


@CordaSerializable
data class NodeInfoContainer(
        var clientId: String = "",
        var fileName: String = "nodeInfo",
        var manipulationServiceNodeId: String = "",
        var lastUpdateDate: Instant = Instant.now(),
        var containerType : com.safexain.nodesharing.model.NodeInfoContainerType = com.safexain.nodesharing.model.NodeInfoContainerType.NODE_INFO,
        var nameItem : String=""
)