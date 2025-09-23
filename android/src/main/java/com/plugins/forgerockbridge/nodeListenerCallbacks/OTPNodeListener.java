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
import com.plugins.forgerockbridge.ErrorHandler;

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
      Log.e(TAG, "[OTPDeleteNodeListener]: public void onException(Exception e)" + e);
      pluginState.reset();
      ErrorHandler.reject(call, ErrorHandler.ErrorCode.AUTHENTICATE_FAILED);
    }

    @Override
    public void onSuccess(FRSession frSession) {
        Log.d(TAG, "[onSuccess]");

        pluginState.reset();

        JSObject result = new JSObject();
        result.put("status", "success");
        result.put("message", "Proceso finalizado. Si deseas otro OTP, inicia de nuevo el árbol.");

        call.resolve(result);

    }

    @Override
    public void onCallbackReceived(Node node) {

        try {
            Log.d(TAG, "onCallbackReceived");
            String uri = null;
            boolean hasTextOutput = false;
            boolean hasHiddenValue = false;
            node.getHeader();
             for (Callback cb : node.getCallbacks()) {
             
                if (cb instanceof HiddenValueCallback) {
                    hasHiddenValue = true;
                    uri = ((HiddenValueCallback) cb).getValue();
                    break;
                }else if(cb instanceof TextOutputCallback){
                    int messageType = ((TextOutputCallback) cb).getMessageType();
                    if(messageType == 2){
                        hasTextOutput = true;
                        break;
                    }
                }
            }

           if(hasHiddenValue){
               Log.d(TAG, "ENTRO AL HIDDEN");
               registerMechanism(uri, node);
           }else if (hasTextOutput) {
               Log.e(TAG, "hasTextOutput");
             ErrorHandler.reject(call, ErrorHandler.ErrorCode.CALLBACK_FAILED);
             pluginState.reset();
           }
            node.next(context, OTPNodeListener.this);
        } catch (Exception e) {
            Log.e(TAG, "Exception CALLBACK_FAILED"+e);
          ErrorHandler.reject(call, ErrorHandler.ErrorCode.CALLBACK_FAILED);
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
                Log.d(TAG, "VINO AL ON SUCCES");
                    node.next(context, OTPNodeListener.this);
            }

            @Override
            public void onException(Exception e) {
              Log.e(TAG, "[ALGO PASO] "+e);
              ErrorHandler.reject(call, ErrorHandler.ErrorCode.REGISTER_OTP_FAILED);
              pluginState.reset();
            }
        });
    }
}
