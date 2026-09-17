package org.telegram.mercurynostr;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;

/**
 * First real (non-hardcoded) Nostr login: paste an nsec, fetch that pubkey's
 * own kind-0 profile, show it. Only login method wired up so far -- bunker/
 * QR/NIP-55/generate-new-identity are Milestone 2 proper, not this. Storing
 * the raw nsec only in a local var (never persisted) is deliberate for the
 * same reason: real key storage is Milestone 2, not this quick spike.
 */
public class NostrLoginActivity extends BaseFragment {

    private static final java.util.List<String> RELAYS = java.util.Arrays.asList(
            "wss://relay.damus.io", "wss://nos.lol", "wss://relay.nostr.band");

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Accedi con Nostr");
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24));

        TextView hint = new TextView(context);
        hint.setText("Incolla la tua chiave privata Nostr (nsec1...)");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        hint.setPadding(0, 0, 0, AndroidUtilities.dp(12));
        layout.addView(hint);

        EditText nsecInput = new EditText(context);
        nsecInput.setHint("nsec1...");
        nsecInput.setSingleLine(true);
        layout.addView(nsecInput);

        TextView status = new TextView(context);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        status.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        layout.addView(status);

        Button loginButton = new Button(context);
        loginButton.setText("Accedi");
        layout.addView(loginButton);

        loginButton.setOnClickListener(v -> {
            String nsec = nsecInput.getText().toString().trim();
            if (nsec.isEmpty()) {
                Toast.makeText(context, "Incolla prima una nsec", Toast.LENGTH_SHORT).show();
                return;
            }
            loginButton.setEnabled(false);
            status.setText("Connessione ai relay...");

            new Thread(() -> {
                NostrProfile profile;
                String error = null;
                try {
                    profile = NostrSpike.loginAndFetchProfileBlocking(nsec, RELAYS);
                } catch (Throwable t) {
                    profile = null;
                    error = t.getMessage() != null ? t.getMessage() : t.toString();
                }
                NostrProfile finalProfile = profile;
                String finalError = error;
                AndroidUtilities.runOnUIThread(() -> {
                    loginButton.setEnabled(true);
                    if (finalError != null) {
                        status.setText("Errore: " + finalError);
                        return;
                    }
                    Bundle args = new Bundle();
                    if (finalProfile != null) {
                        args.putString("npub", finalProfile.getNpub());
                        args.putString("name", finalProfile.getName());
                        args.putString("about", finalProfile.getAbout());
                        args.putString("pictureUrl", finalProfile.getPictureUrl());
                    } else {
                        args.putString("npub", "(pubkey valida, ma nessun profilo kind 0 trovato sui relay)");
                    }
                    presentFragment(new NostrProfileActivity(args));
                });
            }, "nostr-login").start();
        });

        fragmentView = layout;
        return fragmentView;
    }
}
