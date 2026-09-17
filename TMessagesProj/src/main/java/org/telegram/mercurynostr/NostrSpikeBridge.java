package org.telegram.mercurynostr;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;

/**
 * Milestone 1 de-risking spike (see the implementation plan at
 * ~/.claude/plans/ok-f-droid-evitiamo-structured-thompson.md). Proves a real
 * NIP-17 DM can be shimmed into the existing, unmodified chat UI without
 * ever touching MTProto/AccountInstance.
 *
 * Everything here is throwaway: the hardcoded test identity, the fixed
 * synthetic sender id, and the trigger (DialogsActivity.onResume, once per
 * process). None of this is how key management, dialog ids, or the
 * inbound/outbound seams will actually work once this becomes real --
 * this class only exists to answer the spike's go/no-go question.
 */
public class NostrSpikeBridge {

    // THROWAWAY spike-only test identity -- zero reputation/custody stakes,
    // not related to any real user or the Zapstore publisher identity.
    // Replaced entirely by real per-user key management in Milestone 2.
    private static final String TEST_NSEC = "nsec17vxj3en2gcs5xwngtqfftyqnx37xrzc76796vqxkuxu674w4a3sqfla0le";
    private static final List<String> RELAYS = Arrays.asList("wss://relay.damus.io", "wss://nos.lol");

    // Fixed synthetic Telegram-side id for the spike's one test peer.
    private static final long SYNTHETIC_SENDER_ID = 999_777_001L;

    private static final AtomicBoolean started = new AtomicBoolean(false);

    /** Idempotent -- safe to call from a lifecycle method that fires repeatedly. */
    public static void maybeStartSpike(BaseFragment fragment, int account) {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        openTestChat(fragment, account);
        NostrSpike.startListening(TEST_NSEC, RELAYS, dm ->
                AndroidUtilities.runOnUIThread(() -> injectIncomingMessage(account, dm)));
    }

    private static void openTestChat(BaseFragment fragment, int account) {
        seedSyntheticUser(account);
        Bundle args = new Bundle();
        args.putLong("user_id", SYNTHETIC_SENDER_ID);
        args.putBoolean("historyPreloaded", true);
        ChatActivity chatActivity = new ChatActivity(args);
        chatActivity.forceEmptyHistory();
        fragment.presentFragment(chatActivity);
    }

    // Pure in-memory ConcurrentHashMap put -- never touches MessagesStorage
    // or ConnectionsManager. See the plan's "Decisione architetturale"
    // section for why this specific, narrow touch-point is safe.
    private static void seedSyntheticUser(int account) {
        TLRPC.TL_user user = new TLRPC.TL_user();
        user.id = SYNTHETIC_SENDER_ID;
        user.access_hash = 1L;
        user.first_name = "Nostr Spike";
        user.flags = TLObject.setFlag(user.flags, TLObject.FLAG_0, true); // access_hash present
        user.flags = TLObject.setFlag(user.flags, TLObject.FLAG_1, true); // first_name present
        MessagesController.getInstance(account).putUser(user, true);
    }

    private static void injectIncomingMessage(int account, ReceivedDm dm) {
        seedSyntheticUser(account);

        TLRPC.TL_message message = new TLRPC.TL_message();
        message.id = (int) (System.currentTimeMillis() / 1000L);
        message.date = (int) dm.getCreatedAtSecs();
        message.message = dm.getText();
        message.dialog_id = SYNTHETIC_SENDER_ID;

        TLRPC.TL_peerUser peer = new TLRPC.TL_peerUser();
        peer.user_id = SYNTHETIC_SENDER_ID;
        message.peer_id = peer;

        TLRPC.TL_peerUser from = new TLRPC.TL_peerUser();
        from.user_id = SYNTHETIC_SENDER_ID;
        message.from_id = from;
        message.flags = TLObject.setFlag(message.flags, TLObject.FLAG_8, true); // from_id present

        MessageObject messageObject = new MessageObject(account, message, true, true);
        ArrayList<MessageObject> arr = new ArrayList<>();
        arr.add(messageObject);

        // Same notification the real MTProto receive path fires -- lets
        // ChatActivity's own existing handling (row bookkeeping, scroll,
        // unread badge) do the work instead of poking its fields directly.
        NotificationCenter.getInstance(account).postNotificationName(
                NotificationCenter.didReceiveNewMessages, SYNTHETIC_SENDER_ID, arr, false, 0);
    }
}
