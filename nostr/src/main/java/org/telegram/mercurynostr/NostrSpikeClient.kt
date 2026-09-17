package org.telegram.mercurynostr

import kotlinx.coroutines.runBlocking
import org.nostrdevkit.sdk.Client
import org.nostrdevkit.sdk.Filter
import org.nostrdevkit.sdk.Keys
import org.nostrdevkit.sdk.Kind
import org.nostrdevkit.sdk.PublicKey
import org.nostrdevkit.sdk.ReqTarget
import org.nostrdevkit.sdk.RelayUrl
import org.nostrdevkit.sdk.UnwrappedGift
import org.nostrdevkit.sdk.nip17MakePrivateMsg

private const val KIND_GIFT_WRAP = 1059uL

/** Milestone 1 spike result: one decrypted NIP-17 DM. */
data class ReceivedDm(
    val senderPubkeyHex: String,
    val text: String,
    val createdAtSecs: Long,
)

/**
 * Minimal, self-contained NIP-17 client for the Milestone 1 de-risking spike
 * (see the implementation plan). Deliberately has no knowledge of Telegram's
 * data model — the glue code that turns a [ReceivedDm] into a synthetic
 * TLRPC message lives in TMessagesProj, not here.
 */
class NostrSpikeClient(private val keys: Keys, relayUrls: List<String>) {

    private val client = Client()
    private val relays = relayUrls.map { RelayUrl.parse(it) }

    fun myPubkeyHex(): String = keys.publicKey().toHex()

    suspend fun connect() {
        for (relay in relays) client.addRelay(relay)
        client.connect()
    }

    /**
     * Blocks (suspends) until exactly one gift-wrapped DM addressed to this
     * client's pubkey arrives, then unwraps and returns it. Spike scope only
     * one message; a real implementation would keep streaming.
     */
    suspend fun awaitOneDirectMessage(): ReceivedDm {
        val filter = Filter().kind(Kind(KIND_GIFT_WRAP)).pubkey(keys.publicKey())
        val stream = client.streamEvents(ReqTarget.auto(listOf(filter)))
        while (true) {
            val item = stream.next() ?: continue
            val giftWrap = item.event ?: continue
            val unwrapped = UnwrappedGift.fromGiftWrap(keys, giftWrap)
            val rumor = unwrapped.rumor()
            return ReceivedDm(
                senderPubkeyHex = unwrapped.sender().toHex(),
                text = rumor.content(),
                createdAtSecs = rumor.createdAt().asSecs().toLong(),
            )
        }
    }

    suspend fun sendDirectMessage(receiverPubkeyHex: String, text: String) {
        val receiver = PublicKey.parse(receiverPubkeyHex)
        val event = nip17MakePrivateMsg(keys, receiver, text, null, emptyList())
        client.sendEvent(event)
    }
}

/** Java-callable (SAM) callback — Java has no suspend-fun interop. */
fun interface DmCallback {
    fun onReceived(dm: ReceivedDm)
}

/**
 * Java-friendly, blocking entry points over [NostrSpikeClient] for the
 * Milestone 1 spike glue code in TMessagesProj (org.telegram.mercurynostr.
 * NostrSpikeBridge). Callers are expected to invoke these on a background
 * thread themselves -- nothing here switches threads on its own.
 */
object NostrSpike {
    @JvmStatic
    fun startListening(nsec: String, relayUrls: List<String>, callback: DmCallback) {
        Thread({
            runBlocking {
                val client = NostrSpikeClient(Keys.parse(nsec), relayUrls)
                client.connect()
                callback.onReceived(client.awaitOneDirectMessage())
            }
        }, "nostr-spike-listen").start()
    }

    @JvmStatic
    fun sendBlocking(nsec: String, relayUrls: List<String>, receiverPubkeyHex: String, text: String) {
        runBlocking {
            val client = NostrSpikeClient(Keys.parse(nsec), relayUrls)
            client.connect()
            client.sendDirectMessage(receiverPubkeyHex, text)
        }
    }
}
