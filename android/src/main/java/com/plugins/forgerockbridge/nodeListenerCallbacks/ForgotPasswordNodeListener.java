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
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.TextOutputCallback;
import org.forgerock.android.auth.callback.PasswordCallback;

import com.plugins.forgerockbridge.ErrorHandler;


public class ForgotPasswordNodeListener implements NodeListener<FRSession> {

    private static final String TAG = "ForgeRockBridge";
    private final PluginCall call;
    private final Context context;
    private final PluginState pluginState;

    public ForgotPasswordNodeListener(PluginCall call, Context context, PluginState pluginState) {
        this.call = call;
        this.context = context;
        this.pluginState = pluginState;
    }
    @Override
    public void onException(@NonNull Exception e) {
      Log.d(TAG, "[ForgotPasswordNodeListener]: public void onException(Exception e)" + e);
      pluginState.reset();
      ErrorHandler.reject(call, ErrorHandler.ErrorCode.UNKNOWN_ERROR, "UNKNOW ERROR");
    }

    @Override
    public void onSuccess(FRSession frSession) {
        Log.d(TAG, "[ForgotPasswordNodeListener: onSuccess] call method onSuccess");
        pluginState.reset();
        JSObject result = new JSObject();
        result.put("status", "success");
        result.put("message", "Password changed successfully");
        call.resolve(result);
    }

    @Override
    public void onCallbackReceived(Node node) {

        try {
            Log.d(TAG, "[ForgotPasswordNodeListener: onCallbackReceived]: call method onCallbackReceived");
            
            final String username = call.getString("username", null);
            final String answer = call.getString("answer", null);
            
            boolean hasName = false;
            boolean hasTextOutput = false;
            boolean hasQuestion = false;
            String errorMessage = null;

            for (Callback cb : node.getCallbacks()) {
                if (cb instanceof TextOutputCallback) {
                    errorMessage = ((TextOutputCallback) cb).getMessage();
                    Log.d(TAG, "[ForgotPasswordNodeListener: onCallbackReceived] errorMessage: "+ errorMessage);
                    pluginState.setLastErrorMessage(errorMessage);
                    hasTextOutput = true;
                } else if (cb instanceof NameCallback) {
                    Log.d(TAG, "[ForgotPasswordNodeListener: onCallbackReceived] Has NameCallBack node");
                    hasName = true;
                } else if (cb instanceof PasswordCallback) {
                    Log.d(TAG, "[ForgotPasswordNodeListener: onCallbackReceived] Has PasswordCallback node");
                    hasQuestion = true;
                }
            }

            //Comprobar campos errores regresado de ForgeRock y devolverlos al front-end
            if (hasTextOutput) {
                JSObject result = new JSObject();
                result.put("status", "error");
                result.put("message", errorMessage);
                call.resolve(result);
                return;
            }

            //Paso 1 - comprobar la existencia del campo para el usuario y enviarlo
            if (hasName) {
                for (Callback cb : node.getCallbacks()) {
                    if (cb instanceof NameCallback) {
                        ((NameCallback) cb).setName(username);
                    } 
                }
                Log.d(TAG, "[ForgotPasswordNodeListener: onCallbackReceived] Sending credentials again via next()");
                node.next(context, this);
                return;
            }

            //Paso 2 luego de verificar el usuario, guardar la pregunta devuelta para enviarla al front-end
            if (hasQuestion && answer == null) {
                pluginState.setPendingNode(node);
                JSObject out = new JSObject()
                    .put("status", "success")
                    .put("message", "verified username");
                call.resolve(out);
                return;
            }

            //Paso 3 pregunta contestada correctamente, poner el node en pending para seguir el flujo
            if (answer != null) {
                pluginState.setPendingNode(node);
                JSObject out = new JSObject()
                    .put("status", "success")
                    .put("message", "question success");
                call.resolve(out);
                return;
            }

            JSObject out = new JSObject()
            .put("status", "error")
            .put("message", "Unhandled node state");
            call.resolve(out);

        } catch (Exception e) {
            Log.d(TAG, "[ForgotPasswordNodeListener: onCallbackReceived] error catch (Exception e)");
            call.reject("Error processing node: " + e.getMessage(), e);
        }

    }
}
