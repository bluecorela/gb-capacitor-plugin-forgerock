package com.plugins.forgerockbridge.nodeListenerCallbacks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.plugins.forgerockbridge.state.PluginState;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.SSOToken;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.callback.TextOutputCallback;
import org.forgerock.android.auth.callback.HiddenValueCallback;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.*;


import org.json.JSONArray;

import java.util.List;
import java.util.Objects;

public class OTPDeleteNodeListener implements NodeListener<FRSession> {

    private static final String TAG = "ForgeRockBridge";
    private final PluginCall call;
    private final Context context;
    private final PluginState pluginState;

    public OTPDeleteNodeListener(PluginCall call, Context context, PluginState pluginState) {
        this.call = call;
        this.context = context;
        this.pluginState = pluginState;
    }

    @Override
    public void onCallbackReceived(Node node) {
        try {
            Log.d(TAG, "[this.call Delete] this.3call: " + this.call);
            Log.d(TAG, "[node] node: " );
            Log.d(TAG, "[AuthNodeListener] errorMessage: " );
        } catch (Exception e) {
            Log.d(TAG, "[AuthNodeListener] error catch (Exception e)");
            pluginState.reset();
            call.reject("Error processing node: " + e.getMessage(), e);
        }
    }

    @Override
    public void onException(@NonNull Exception e) {
        Log.d(TAG, "[AuthNodeListener]: public void onException(Exception e)" + e);
        pluginState.reset();
        call.reject("authenticate failed: " + e.getMessage(), e);
    }

    @Override
    public void onSuccess(FRSession frSession) {
        try {

            JSObject result = new JSObject();
            result.put("status", "success");
            result.put("message", "OTP eliminado correctamente");
            call.resolve(result);

            deleteOtpRegister();

        } catch (Exception e) {
            Log.e(TAG, "Error eliminando OTP", e);
            call.reject("Fallo al eliminar OTP: " + e.getMessage());
        }
        pluginState.reset();

    }

    private void deleteOtpRegister() {
        try {
            FRAClient fraClient = new FRAClient.FRAClientBuilder()
                    .withContext(this.context)
                    .start();

            List<Account> accounts = fraClient.getAllAccounts();
            if (!accounts.isEmpty()) {
                Account account = accounts.get(0);

                List<Mechanism> mechanisms = account.getMechanisms();
                if (!mechanisms.isEmpty()) {
                    Mechanism mechanism = mechanisms.get(0);
                    fraClient.removeMechanism(mechanism);

                    Log.d(TAG, "OTP eliminado para: " + account.getAccountName());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "[startOtpTreeAuthentication] authenticate error", e);
            call.reject("authenticate failed: " + e.getMessage(), e);
        }
    }
}
