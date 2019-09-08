package com.safexain.nodesharing.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.safexain.nodesharing.contract.NodeShareContract.Companion.NODE_INFO_CONTRACT_ID
import java.time.Instant


@InitiatingFlow
@StartableByRPC
class NodeInfoShareFlow(val party: String,
                        var clientId: String = "",
                        var fileName: String = "nodeInfo",
                        var manipulationServiceNodeId: String = "",
                        var containerType: com.safexain.nodesharing.model.NodeInfoContainerType = com.safexain.nodesharing.model.NodeInfoContainerType.NODE_INFO,
                        var nameItem: String = ""
) : FlowLogic<SignedTransaction>() {

    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new IOU.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.GENERATING_TRANSACTION,
                com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.VERIFYING_TRANSACTION,
                com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.SIGNING_TRANSACTION,
                com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.GATHERING_SIGS,
                com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.FINALISING_TRANSACTION
        )
    }

    override val progressTracker = com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.tracker()

    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val me = serviceHub.myInfo.legalIdentities.first()

        val target = serviceHub.identityService.partiesFromName(party, true)
        if (target.isEmpty()) {
            throw IllegalArgumentException("Illegal Buyer specified!")
        }
        val targetParty = target.iterator().next()
        if (targetParty == ourIdentity) {
            throw IllegalArgumentException("Illegal Buyer specified!")
        }

        // Stage 1.
        progressTracker.currentStep = com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.GENERATING_TRANSACTION

        val nodeInfo = com.safexain.nodesharing.model.NodeInfoContainer(clientId,
                fileName,
                manipulationServiceNodeId,
                Instant.now(),
                containerType, nameItem)

        val state = com.safexain.nodesharing.state.NodeInfoShareState(me, targetParty, nodeInfo)

        val txCommand = Command(com.safexain.nodesharing.contract.NodeShareContract.Commands.SendNodeInfo(), listOf(me.owningKey, targetParty.owningKey))
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(state, NODE_INFO_CONTRACT_ID)
                .addCommand(txCommand)

        // Stage 2.
        progressTracker.currentStep = com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.VERIFYING_TRANSACTION
        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Stage 3.
         progressTracker.currentStep = com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.SIGNING_TRANSACTION
        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Stage 4.
        progressTracker.currentStep = com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.GATHERING_SIGS
        // SendNodeInfo the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(targetParty)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.GATHERING_SIGS.childProgressTracker()))

        //Stage 5.
        progressTracker.currentStep = com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.FINALISING_TRANSACTION

        // Notarise and record the transaction in both parties' vaults.
        return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession), com.safexain.nodesharing.flow.NodeInfoShareFlow.Companion.FINALISING_TRANSACTION.childProgressTracker()))
    }
}

@InitiatedBy(com.safexain.nodesharing.flow.NodeInfoShareFlow::class)
class NodeInfoContainerFlResponder(val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                ///  val output = stx.tx.outputs.single().data
                ///    "This must be an Yo transaction." using (output is YoState)

            }
        }
        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(counterPartySession, expectedTxId = txId))
    }
}