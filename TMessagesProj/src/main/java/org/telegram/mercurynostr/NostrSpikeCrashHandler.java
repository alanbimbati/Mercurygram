package org.telegram.mercurynostr;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Diagnostic-only uncaught exception handler for the current de-risking
 * phase: shows the crash on screen (CrashActivity) instead of just the
 * generic system dialog, since there's no adb/device connection to read
 * logcat from otherwise. Remove once past the phase where this subsystem
 * is expected to crash hard and unpredictably.
 */
public class NostrSpikeCrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context appContext;

    public NostrSpikeCrashHandler(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        String trace;
        try {
            trace = Log.getStackTraceString(throwable);
        } catch (Throwable ignored) {
            trace = String.valueOf(throwable);
        }
        try {
            Intent intent = new Intent(appContext, CrashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("trace", trace);
            appContext.startActivity(intent);
        } catch (Throwable ignored) {
            // If even this fails, fall through to killing the process below --
            // better a silent crash than a hang.
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
