package com.safexain.cordapp.contract

import com.safexain.cordapp.state.NodeInfoState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class NodeInfoContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<NodeInfoContract.Commands>()

        when (command.value) {
            is Commands.Send -> requireThat {
                "No inputs should be consumed when issuing an  send." using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)
                val nodeInfoState = tx.outputsOfType<NodeInfoState>().single()
                "No sending Yo's to yourself!" using (nodeInfoState.origin != nodeInfoState.target)
                val notary = tx.notary
                "Is not possible send message to notary" using (nodeInfoState.target != notary)
            }
            is Commands.Read -> requireThat {
                "Only one input state should be created." using (tx.inputs.size == 1)
            }
        }
    }


    companion object {
        @JvmStatic
        val NODE_INFO_CONTRACT_ID = "com.safexain.cordapp.contract"
    }

     interface Commands : CommandData {
         class Send : Commands
         class Read : Commands
    }
}
