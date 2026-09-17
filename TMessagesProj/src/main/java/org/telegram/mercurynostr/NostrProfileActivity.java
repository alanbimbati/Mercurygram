package org.telegram.mercurynostr;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

/**
 * First real (non-hardcoded) Nostr screen: shows whatever kind-0 profile
 * NostrLoginActivity fetched for the pubkey the user just logged in with.
 * Deliberately plain Android widgets, not Telegram's themed components --
 * this is "prove real data renders", not final UI polish.
 */
public class NostrProfileActivity extends BaseFragment {

    public NostrProfileActivity(Bundle args) {
        super(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Profilo Nostr");
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24));

        Bundle args = getArguments();
        String name = args != null ? args.getString("name") : null;
        String about = args != null ? args.getString("about") : null;
        String npub = args != null ? args.getString("npub") : null;
        String pictureUrl = args != null ? args.getString("pictureUrl") : null;

        addLine(context, layout, name != null ? name : "(nessun nome nel profilo)", 22, true);
        addLine(context, layout, npub, 13, false);
        if (about != null) {
            addLine(context, layout, about, 16, false);
        }
        if (pictureUrl != null) {
            addLine(context, layout, "Foto profilo: " + pictureUrl, 13, false);
        }

        fragmentView = layout;
        return fragmentView;
    }

    private void addLine(Context context, LinearLayout layout, String text, int spSize, boolean bold) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, spSize);
        tv.setGravity(Gravity.START);
        if (bold) {
            tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        tv.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        layout.addView(tv);
    }
}
