package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.training.contract.NodeInfoContract
import net.corda.training.contract.NodeInfoContract.Companion.NODE_INFO_CONTRACT_ID
import net.corda.training.model.CordappShareContainer
import net.corda.training.model.StatusEnum
import net.corda.training.state.CordapShareState
import net.corda.training.state.NodeInfoShareState
import java.time.Instant


@InitiatingFlow
@StartableByRPC
class CordapShareFlow(val party: String,
                      var fileId: String = "cordapp",
                      var owner: String = "",
                      var manipulationServiceNodeId: String = "",
                      var frontGitUrl: String = "url",
                      var sharingPartyIds: MutableList<String> = emptyList<String>().toMutableList(),
                      var image: ByteArray? = null,
                      var status: String = StatusEnum.ACTIVE.toString(),
                      var isEditable: Boolean = false
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
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

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
        progressTracker.currentStep = GENERATING_TRANSACTION

        val cordapShareContainer = CordappShareContainer(
                fileId,
                owner,
                manipulationServiceNodeId,
                frontGitUrl,
                sharingPartyIds,
                Instant.now(),
                image,
                status,
                isEditable
        )

        val state = CordapShareState(me, targetParty, cordapShareContainer)

        val txCommand = Command(NodeInfoContract.Commands.Send(), listOf(me.owningKey, targetParty.owningKey))
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(state, NODE_INFO_CONTRACT_ID)
                .addCommand(txCommand)

        // Stage 2.
        progressTracker.currentStep = VERIFYING_TRANSACTION
        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Stage 3.
        progressTracker.currentStep = SIGNING_TRANSACTION
        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Stage 4.
        progressTracker.currentStep = GATHERING_SIGS
        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(targetParty)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), GATHERING_SIGS.childProgressTracker()))

        //Stage 5.
        progressTracker.currentStep = FINALISING_TRANSACTION

        // Notarise and record the transaction in both parties' vaults.
        return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))
    }
}

@InitiatedBy(CordapShareFlow::class)
class CordapShareResponder(val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
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