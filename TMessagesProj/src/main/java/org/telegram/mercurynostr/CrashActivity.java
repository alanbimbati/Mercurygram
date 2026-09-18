package org.telegram.mercurynostr;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Standalone (not a Telegram BaseFragment/Theme-dependent screen) crash
 * display, launched by NostrSpikeCrashHandler when the app would otherwise
 * just die with the generic system "keeps stopping" dialog. Text is
 * selectable so it can be copied out and shared without needing adb.
 * Diagnostic tool for the current de-risking phase -- remove once the
 * Nostr subsystem is past its early, likely-to-crash-hard stage.
 */
public class CrashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String trace = getIntent() != null ? getIntent().getStringExtra("trace") : null;
        if (trace == null) {
            trace = "(nessuno stack trace disponibile)";
        }

        TextView textView = new TextView(this);
        textView.setText(trace);
        textView.setTextIsSelectable(true);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView.setPadding(24, 24, 24, 24);
        textView.setMovementMethod(new ScrollingMovementMethod());

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);
        setContentView(scrollView);
    }
}
