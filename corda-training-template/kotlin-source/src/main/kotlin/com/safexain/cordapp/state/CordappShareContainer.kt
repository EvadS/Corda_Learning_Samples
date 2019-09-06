package com.safexain.cordapp.state

import java.time.Instant

data class CordappShareContainer(

        var fileId: String = "cordapp",
        var owner: String = "",
        var manipulationServiceNodeId: String = "",
        var frontGitUrl: String = "url",
        var sharingPartyIds: MutableList<String> = emptyList<String>().toMutableList(),
        var lastUpdateDate: Instant = Instant.now(),
        var image: ByteArray? = null,
        var status: String = StatusEnum.ACTIVE.toString(),
        var isEditable: Boolean = false

)