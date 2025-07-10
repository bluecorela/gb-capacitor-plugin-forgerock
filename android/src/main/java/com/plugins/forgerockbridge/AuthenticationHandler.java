package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;
import com.getcapacitor.PluginCall;
import org.forgerock.android.auth.FRSession;
import com.plugins.forgerockbridge.nodeListenerCallbacks.AuthNodeListener;

public class AuthenticationHandler {

    private static final String TAG = "[AuthenticationHandler]";

    public static void handle(PluginCall call, Context context) {
        try {
            String journey = call.getString("journey");

            if (journey == null) {
                call.reject("Missing journey name");
                return;
            }

            authenticate(context, journey, new AuthNodeListener(call, context));
        } catch (Exception e) {
            Log.e(TAG, "Authentication error", e);
            call.reject("Authentication failed: " + e.getMessage(), e);
        }
    }


    public static void authenticate(Context context, String journeyName, AuthNodeListener listener) {
        try {
            Log.d(TAG, "Starting authentication with journey: " + journeyName);
            FRSession.authenticate(context, journeyName, listener);
        } catch (Exception e) {
            Log.e(TAG, "Error starting authentication", e);
            listener.getCall().reject("Error starting authentication: " + e.getMessage(), e);
        }
    }
}
