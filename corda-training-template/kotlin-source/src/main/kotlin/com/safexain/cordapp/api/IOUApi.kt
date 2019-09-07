package com.safexain.cordapp.api


import com.safexain.cordapp.flow.NodeInfoContainerFlow

import com.safexain.cordapp.state.IOUState
import com.safexain.cordapp.state.NodeInfoState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.utilities.loggerFor
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.workflows.getCashBalances
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * This API is accessible from /api/iou. The endpoint paths specified below are relative to it.
 * We've defined a bunch of endpoints to deal with IOUs, cash and the various operations you can perform with them.
 */
@Path("iou")
class IOUApi(val rpcOps: CordaRPCOps) {
    private val me = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<IOUApi>()
    }

    fun X500Name.toDisplayString() : String  = BCStyle.INSTANCE.toString(this)

    /** Helpers for filtering the network map cache. */
    private fun isNotary(nodeInfo: NodeInfo) = rpcOps.notaryIdentities().any { nodeInfo.isLegalIdentity(it) }
    private fun isMe(nodeInfo: NodeInfo) = nodeInfo.legalIdentities.first().name == me
    private fun isNetworkMap(nodeInfo : NodeInfo) = nodeInfo.legalIdentities.single().name.organisation == "Network Map Service"

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to me.toString())

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<String>> {
        return mapOf("peers" to rpcOps.networkMapSnapshot()
                .filter { isNotary(it).not() && isMe(it).not() && isNetworkMap(it).not() }
                .map { it.legalIdentities.first().name.toX500Name().toDisplayString() })
    }

    /**
     * Task 1
     * Displays all IOU states that exist in the node's vault.
     * TODO: Return a list of IOUStates on ledger
     * Hint - Use [rpcOps] to query the vault all unconsumed [IOUState]s
     */
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    fun getIOUs(): List<StateAndRef<ContractState>> {
        // Filter by state type: IOU.
        return rpcOps.vaultQueryBy<IOUState>().states
    }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GET
    @Path("cash")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCash(): List<StateAndRef<ContractState>> {
        // Filter by state type: Cash.
        return rpcOps.vaultQueryBy<Cash.State>().states
    }


    /**
     * Task 1
     * Displays all cars states that exist in the node's vault.
     * TODO: Return a list of IOUStates on ledger
     * Hint - Use [rpcOps] to query the vault all unconsumed [CarState]s
     */
    @GET
    @Path("infos")
    @Produces(MediaType.APPLICATION_JSON)
    fun geInfos(): List<StateAndRef<ContractState>> {
        println("===  cars === ");
        // Filter by state type: IOU.

        var pageNum  = 0
        var pageSize  = 10
        var pageSpec  = PageSpecification(pageNum ,pageSize)
        return rpcOps.vaultQueryBy<NodeInfoState>().states
    }


    //TODO
    @PUT
    @Path("{buyer}/{amount}/{currency}/issue-car")
    fun issueCar(message: String,
                 @QueryParam(value = "target") party: String): Response {

        rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(party)) ?:
        throw IllegalArgumentException("Unknown party name.")
        try {

            val result = rpcOps.startFlowDynamic(NodeInfoContainerFlow::class.java
                    ,message
                    , party
            ).returnValue.get()

            // Return the response.
            return Response
                    .status(Response.Status.CREATED)
                    .entity("Transaction id ${result.id} committed to ledger.\n${result.tx.outputs.single()}")
                    .build()
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (e: Exception) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.message)
                    .build()
        }
    }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GET
    @Path("cash-balances")
    @Produces(MediaType.APPLICATION_JSON)
            // Display cash balances.
    fun getCashBalances() = rpcOps.getCashBalances()

    /**
     * Initiates a flow to agree an IOU between two parties.
     */
//    @GET
//    @Path("issue-iou")
//    fun issueIOU(@QueryParam(value = "amount") amount: Int,
//                 @QueryParam(value = "currency") currency: String,
//                 @QueryParam(value = "party") party: String): Response {
//        // Get party objects for myself and the counterparty.
//        val me = rpcOps.nodeInfo().legalIdentities.first()
//        val lender = rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(party)) ?: throw IllegalArgumentException("Unknown party name.")
//        // Create a new IOU state using the parameters given.
//        try {
//            val state = IOUState(Amount(amount.toLong() * 100, Currency.getInstance(currency)), lender, me)
//            // Start the IOUIssueFlow. We block and waits for the flow to return.
//            val result = rpcOps.startTrackedFlow(::IOUIssueFlow, state).returnValue.get()
//            // Return the response.
//            return Response
//                    .status(Response.Status.CREATED)
//                    .entity("Transaction id ${result.id} committed to ledger.\n${result.tx.outputs.single()}")
//                    .build()
//            // For the purposes of this demo app, we do not differentiate by exception type.
//        } catch (e: Exception) {
//            return Response
//                    .status(Response.Status.BAD_REQUEST)
//                    .entity(e.message)
//                    .build()
//        }
//    }

    /**
     * Transfers an IOU specified by [linearId] to a new party.
     */
//    @GET
//    @Path("transfer-iou")
//    fun transferIOU(@QueryParam(value = "id") id: String,
//                    @QueryParam(value = "party") party: String): Response {
//        val linearId = UniqueIdentifier.fromString(id)
//        val newLender = rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(party)) ?: throw IllegalArgumentException("Unknown party name.")
//        try {
//            rpcOps.startFlow(::IOUTransferFlow, linearId, newLender).returnValue.get()
//            return Response.status(Response.Status.CREATED).entity("IOU $id transferred to $party.").build()
//
//        } catch (e: Exception) {
//            return Response
//                    .status(Response.Status.BAD_REQUEST)
//                    .entity(e.message)
//                    .build()
//        }
//    }

    /**
     * Settles an IOU. Requires cash in the right currency to be able to settle.
     * Example request:
     * curl -X PUT 'http://localhost:10007/api/iou/issue-iou?amount=99&currency=GBP&party=O=ParticipantC,L=New%20York,C=US
     */
//    @GET
//    @Path("settle-iou")
//    fun settleIOU(@QueryParam(value = "id") id: String,
//                  @QueryParam(value = "amount") amount: Int,
//                  @QueryParam(value = "currency") currency: String): Response {
//        val linearId = UniqueIdentifier.fromString(id)
//        val settleAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))
//
//        try {
//            rpcOps.startFlow(::IOUSettleFlow, linearId, settleAmount).returnValue.get()
//            return Response.status(Response.Status.CREATED).entity("$amount $currency paid off on IOU id $id.").build()
//
//        } catch (e: Exception) {
//            return Response
//                    .status(Response.Status.BAD_REQUEST)
//                    .entity(e.message)
//                    .build()
//        }
//    }

    /**
     * Helper end-point to issue some cash to ourselves.
     */
//    @GET
//    @Path("self-issue-cash")
//    fun selfIssueCash(@QueryParam(value = "amount") amount: Int,
//                      @QueryParam(value = "currency") currency: String): Response {
//        val issueAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))
//
//        try {
//            val cashState = rpcOps.startFlow(::SelfIssueCashFlow, issueAmount).returnValue.get()
//            return Response.status(Response.Status.CREATED).entity(cashState.toString()).build()
//
//        } catch (e: Exception) {
//            return Response
//                    .status(Response.Status.BAD_REQUEST)
//                    .entity(e.message)
//                    .build()
//        }
//    }
}