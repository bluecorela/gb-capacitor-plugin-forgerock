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
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.TextOutputCallback;
import org.forgerock.android.auth.callback.HiddenValueCallback;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.*;


import org.forgerock.android.auth.exception.AuthenticatorException;
import org.json.JSONArray;

import java.util.List;

public class OTPNodeListener implements NodeListener<FRSession> {

    private static final String TAG = "ForgeRockBridge";
    private final PluginCall call;
    private final Context context;
    private final PluginState pluginState;

    public OTPNodeListener(PluginCall call, Context context, PluginState pluginState) {
        this.call = call;
        this.context = context;
        this.pluginState = pluginState;
    }
    @Override
    public void onException(@NonNull Exception e) {
        Log.d(TAG, "[AuthNodeListener]: public void onException(Exception e)" + e);
        pluginState.reset();
        call.reject("authenticate failed: " + e.getMessage(), e);
    }

    @Override
    public void onSuccess(FRSession frSession) {
        pluginState.reset();
        call.resolve();
    }

    @Override
    public void onCallbackReceived(Node node) {
        try {
            String errorMessage = null;
            String uri = null;
            boolean hasTextOutput = false;
            boolean hasHiddenValue = false;

             for (Callback cb : node.getCallbacks()) { 
                if (cb instanceof HiddenValueCallback) {
                    hasHiddenValue = true;
                    uri = ((HiddenValueCallback) cb).getValue();
                    break;
                }else if(cb instanceof TextOutputCallback){
                    int messageType = ((TextOutputCallback) cb).getMessageType();
                    if(messageType == 2){
                        hasTextOutput = true;
                        errorMessage = ((TextOutputCallback) cb).getMessage();
                        break;
                    }
                }
            }

           if(hasHiddenValue){
               registerMechanism(uri, node);
           }else if (hasTextOutput) {
               Log.d(TAG, "[AuthNodeListener] errorMessage: " + errorMessage);
               call.reject("Error processing node: " + errorMessage);
               pluginState.reset();
           }
           
        } catch (Exception e) {
            Log.d(TAG, "[AuthNodeListener] error catch (Exception e)");
            call.reject("Error processing node: " + e.getMessage(), e);
            pluginState.reset();
        }
    }

    private void registerMechanism(String uri, Node node) throws AuthenticatorException {

        FRAClient fraClient = new FRAClient.FRAClientBuilder()
        .withContext(this.context)
        .start();

        fraClient.createMechanismFromUri(uri, new FRAListener<Mechanism>() {
            @Override
            public void onSuccess(Mechanism mechanism) {
                    
                    // Next to finish process register in FR
                    node.next(context, null);

                    JSObject result = new JSObject();
                    result.put("status", "success");
                    result.put("message", "OTP registrado correctamente");

                    Log.d(TAG, "RESULT"+String.valueOf(result));

                    call.resolve(result);
                    pluginState.reset();
               
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "Error registrando OTP: " + e.getMessage(), e);
                call.reject("Error creando mecanismo OTP: " + e.getMessage(), e);
                pluginState.reset();
            }
        });
    }
}
