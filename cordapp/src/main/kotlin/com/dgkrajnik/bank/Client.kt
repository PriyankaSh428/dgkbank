package com.dgkrajnik.bank

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.time.Instant

/**
 * Dump the vault contents, for debugging.
 */
fun main(args: Array<String>) {
    DanielClient().main(args)
}

private class DanielClient {
    companion object {
        val logger: Logger = loggerFor<DanielClient>()
        private fun logState(state: StateAndRef<DanielState>) = logger.info("{}", state.state.data)
    }

    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: DanielClient <node address> <thought>`" }
        val nodeAddress = parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        val proxy = client.start("user1", "test").proxy

        val (snapshot, updates) = proxy.vaultTrack(DanielState::class.java)

        proxy.waitUntilNetworkReady().getOrThrow()

        val issuerID = proxy.wellKnownPartyFromX500Name(BOD_NAME) ?: throw IllegalArgumentException("Could not find the issuer node '${BOD_NAME}'.")

        proxy.startFlow(::DanielIssueRequest, args[1], issuerID)
                .returnValue.getOrThrow()

        snapshot.states.forEach { logState(it) }
        updates.subscribe { update ->
            update.produced.forEach { logState(it) }
        }
    }
}
