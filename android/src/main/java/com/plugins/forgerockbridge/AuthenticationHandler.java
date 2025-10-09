package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;
import com.getcapacitor.PluginCall;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;

import com.plugins.forgerockbridge.nodeListenerCallbacks.AuthNodeListener;
import com.plugins.forgerockbridge.state.PluginState;

public class AuthenticationHandler {

    private static final String TAG = "ForgeRockBridge";

    public static void handle(PluginCall call, Context context, PluginState pluginState) {
        try {
            boolean isRetry = call.getBoolean("isRetry", false);
            Log.d(TAG, "[AuthenticationHandler] isRetry in AuthenticationHandler: " + isRetry);

            String journey = call.getString("journey");
            if (journey == null) {
                call.reject("Missing journey name");
                return;
            }

            if (isRetry && pluginState.getPendingNode() != null) {
                Log.d(TAG, "[AuthenticationHandler] Retrying with pendingNode");
                pluginState.getPendingNode().next(context, new AuthNodeListener(call, context, pluginState));
            } else {
                Log.d(TAG, "[AuthenticationHandler] Starting new authentication with journey: " + journey);
                FRUser.login(context, new AuthNodeListener(call, context, pluginState));
            }

        } catch (Exception e) {
            Log.e(TAG, "[AuthenticationHandler] Authentication error", e);
            call.reject("Authentication failed: " + e.getMessage(), e);
        }
    }
}
