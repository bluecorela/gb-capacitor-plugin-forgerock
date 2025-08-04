package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.exception.AuthenticatorException;

import com.plugins.forgerockbridge.nodeListenerCallbacks.OTPDeleteNodeListener;
import com.plugins.forgerockbridge.nodeListenerCallbacks.OTPNodeListener;
import com.plugins.forgerockbridge.state.PluginState;

import java.util.List;

public class OTPTokenHandler {
    private static final String TAG = "ForgeRockBridge";

    public static void startOtpJourney(PluginCall call, Context context, NodeListener<FRSession> listener ) {
        String journey = call.getString("journey");
        
        if (journey == null) {
            call.reject("Missing journey name");
            return;
        }
        
        Log.d(TAG, "journey" + journey);
        
        try {
            FRSession.authenticate(context, journey, listener);
        } catch (Exception e) {
            Log.e(TAG, "[startOtpTreeAuthentication] authenticate error", e);
            call.reject("[startOtpTreeAuthentication] failed: " + e.getMessage(), e);
        }
    }

    public static void validateOTP(PluginCall call, Context context) {
        try {
            FRAClient fraClient = new FRAClient.FRAClientBuilder()
                    .withContext(context)
                    .start();

            List<Account> accounts = fraClient.getAllAccounts();
            JSObject result = new JSObject();

            result.put("empty", accounts.isEmpty());

            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "[startOtpTreeAuthentication] authenticate error", e);
            call.reject("authenticate failed: " + e.getMessage(), e);
        }
    }

}
