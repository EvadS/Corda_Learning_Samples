package com.safexain.nodesharing.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


class NodeShareContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<com.safexain.nodesharing.contract.NodeShareContract.Commands>()

        when (command.value) {
            is com.safexain.nodesharing.contract.NodeShareContract.Commands.SendNodeInfo -> requireThat {
                "No inputs should be consumed when issuing an  send." using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)
                val nodeInfoState = tx.outputsOfType<com.safexain.nodesharing.state.NodeInfoShareState>().single()
                "No sending Yo's to yourself!" using (nodeInfoState.origin != nodeInfoState.target)
                val notary = tx.notary
                "Is not possible send message to notary" using (nodeInfoState.target != notary)
            }
            is com.safexain.nodesharing.contract.NodeShareContract.Commands.SendCordapp -> requireThat {
                "Only one input state should be created." using (tx.inputs.size == 1)
            }
        }
    }


    companion object {
        @JvmStatic
        val NODE_INFO_CONTRACT_ID = "com.safexain.nodesharing.contract.NodeShareContract"
    }

    interface Commands : CommandData {
        class SendNodeInfo : com.safexain.nodesharing.contract.NodeShareContract.Commands
        class SendCordapp : com.safexain.nodesharing.contract.NodeShareContract.Commands
    }
}