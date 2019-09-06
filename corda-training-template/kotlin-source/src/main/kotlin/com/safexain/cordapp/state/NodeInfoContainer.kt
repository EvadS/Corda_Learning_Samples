package com.safexain.cordapp.state

import java.time.Instant

data class NodeInfoContainer(

        var clientId: String = "",
        var fileName: String = "nodeInfo",
        var manipulationServiceNodeId: String = "",
        var lastUpdateDate: Instant = Instant.now(),
        var containerType : NodeInfoContainerType =  NodeInfoContainerType.NODE_INFO,
        var nameItem : String=""
)

enum class NodeInfoContainerType {
    NODE_INFO,
    UPDATE_CORDAPP
}
