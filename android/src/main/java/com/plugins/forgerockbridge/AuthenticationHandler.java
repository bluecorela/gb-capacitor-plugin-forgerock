package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;
import com.getcapacitor.PluginCall;
import org.forgerock.android.auth.FRSession;
import com.plugins.forgerockbridge.nodeListenerCallbacks.AuthNodeListener;
import com.plugins.forgerockbridge.state.PluginState;

public class AuthenticationHandler {

    private static final String TAG = "ForgeRockBridge";

    // Este método será invocado desde el plugin principal
    public static void handle(PluginCall call, Context context, PluginState pluginState) {
        try {
            boolean isRetry = call.getBoolean("isRetry", false);
            Log.d(TAG, "[AuthenticationHandler] isRetry in AuthenticationHandler" + isRetry);
            if (isRetry) {
                Log.d(TAG, "[AuthenticationHandler] Retrying authentication with stored node");
                if (pluginState.getPendingNode() != null) {
                    Log.d(TAG, "[AuthenticationHandler] About to call pendingNode.next() — Node class: " + pluginState.getPendingNode().getClass().getSimpleName());
                    pluginState.getPendingNode().next(context, new AuthNodeListener(call, context, pluginState)); // ✅ CORRECTO
                    return;
                } else {
                    call.reject("[AuthenticationHandler] No pending node to retry");
                    return;
                }
            }

            String journey = call.getString("journey");
            if (journey == null) {
                call.reject("Missing journey name");
                return;
            }

            Log.d(TAG, "[AuthenticationHandler] Starting new authentication with journey: " + journey);
            FRSession.authenticate(context, journey, new AuthNodeListener(call, context, pluginState));
        } catch (Exception e) {
            Log.e(TAG, "[AuthenticationHandler] Authentication error", e);
            call.reject("Authentication failed: " + e.getMessage(), e);
        }
    }
}
