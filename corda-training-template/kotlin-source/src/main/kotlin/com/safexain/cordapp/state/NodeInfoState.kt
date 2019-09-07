package com.safexain.cordapp.state


import com.safexain.cordapp.contract.IOUContract
import com.safexain.cordapp.contract.NodeInfoContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 * Remove the "val data: String = "data" property before starting the [IOUState] tasks.
 */

@BelongsToContract(NodeInfoContract::class)
data class NodeInfoState(
        val origin: Party,
        val target: Party,
        var message: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants: List<Party> get() = listOf(origin, target)
}


@BelongsToContract(IOUContract::class)
data class IOUState(val amount: Amount<Currency>,
                    val lender: Party,
                    val borrower: Party,
                    val paid: Amount<Currency> = Amount(0, amount.token),
                    override val linearId: UniqueIdentifier = UniqueIdentifier(), override val participants: List<AbstractParty>) : LinearState

