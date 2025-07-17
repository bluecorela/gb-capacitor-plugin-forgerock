package com.plugins.forgerockbridge.nodeListenerCallbacks;

import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.plugins.forgerockbridge.state.PluginState;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.SSOToken;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.callback.TextOutputCallback;

import org.json.JSONArray;

public class AuthNodeListener implements NodeListener<FRSession> {

    private static final String TAG = "ForgeRockBridge";
    private final PluginCall call;
    private final Context context;
    private final PluginState pluginState;

    public AuthNodeListener(PluginCall call, Context context, PluginState pluginState) {
        this.call = call;
        this.context = context;
        this.pluginState = pluginState;
    }

    @Override
    public void onSuccess(FRSession session) {
        try {
            SSOToken token = session.getSessionToken();
            JSObject result = new JSObject()
                    .put("status", "authenticated")
                    .put("token", token.getValue());
            pluginState.reset();
            call.resolve(result);
        } catch (Exception e) {
            pluginState.reset();
            call.reject("Failed to retrieve token: " + e.getMessage(), e);
        }
    }

    @Override
    public void onException(Exception e) {
        Log.d(TAG, "[AuthNodeListener] public void onException(Exception e)");
        pluginState.reset();
        call.reject("Authentication failed: " + e.getMessage(), e);
    }

    @Override
    public void onCallbackReceived(Node node) {
        try {
            final String username = call.getString("username");
            final String password = call.getString("password");

            boolean hasTextOutput = false;
            boolean hasConfirmation = false;
            boolean hasName = false;
            boolean hasPass = false;
            String errorMessage = null;
            JSONArray callbackNames = new JSONArray();

            for (Callback cb : node.getCallbacks()) {
                callbackNames.put(cb.getClass().getSimpleName());
                if (cb instanceof TextOutputCallback) {
                    errorMessage = ((TextOutputCallback) cb).getMessage();
                    Log.d(TAG, "[AuthNodeListener] errorMessage: "+ errorMessage);

                    pluginState.setLastErrorMessage(errorMessage);
                    hasTextOutput = true;
                } else if (cb instanceof ConfirmationCallback) {
                    ((ConfirmationCallback) cb).setSelectedIndex(0); // Siempre seleccionamos OK
                    hasConfirmation = true;
                } else if (cb instanceof NameCallback) {
                    hasName = true;
                } else if (cb instanceof PasswordCallback) {
                    hasPass = true;
                }
            }

            // Paso 1: Si hay error + confirmación → reenviar internamente
            if (hasTextOutput && hasConfirmation) {
                Log.d(TAG, "[AuthNodeListener] TextOutput + Confirmation detected. Sending internal next()");

                node.next(context, new NodeListener<FRSession>() {
                    @Override
                    public void onCallbackReceived(Node nextNode) {
                        boolean hasNextName = false;
                        boolean hasNextPass = false;
                        JSONArray innerCallbacks = new JSONArray();

                        for (Callback cb : nextNode.getCallbacks()) {
                            innerCallbacks.put(cb.getClass().getSimpleName());
                            if (cb instanceof NameCallback) hasNextName = true;
                            if (cb instanceof PasswordCallback) hasNextPass = true;
                        }

                        if (hasNextName && hasNextPass) {
                            pluginState.setPendingNode(nextNode);
                            JSObject result = new JSObject()
                                    .put("status", "awaitingRetry")
                                    .put("errorMessage", pluginState.getLastErrorMessage())
                                    .put("callbacks", innerCallbacks);
                            call.resolve(result);
                        } else {
                            call.reject("Unexpected state after confirmation");
                        }
                    }

                    @Override
                    public void onSuccess(FRSession session) {
                        pluginState.reset();
                        JSObject result = new JSObject()
                                .put("status", "authenticated")
                                .put("token", session.getSessionToken().getValue());
                        call.resolve(result);
                    }

                    @Override
                    public void onException(Exception e) {
                        Log.e(TAG, "[AuthNodeListener] << ERROR after confirmation >> " + e.getMessage(), e);

                        // Verifica si teníamos FRE016 guardado
                        String errorMsg = pluginState.getLastErrorMessage();
                        if (errorMsg != null && errorMsg.contains("FRE016")) {
                            JSObject out = new JSObject()
                                    .put("status", "failure")
                                    .put("errorMessage", "FRE016");

                            Log.d(TAG, "[AuthNodeListener] << RESOLVE FRE016 from onException >> " + out.toString());
                            pluginState.reset();
                            call.resolve(out);
                            return;
                        }

                        pluginState.reset();
                        call.reject("Authentication failed: " + e.getMessage(), e);
                    }
                });
                return;
            }

            // Paso 2: Si llegan credenciales nuevamente → reenviar
            if (hasName && hasPass) {
                for (Callback cb : node.getCallbacks()) {
                    if (cb instanceof NameCallback) {
                        ((NameCallback) cb).setName(username);
                    } else if (cb instanceof PasswordCallback) {
                        ((PasswordCallback) cb).setPassword(password.toCharArray());
                    }
                }
                Log.d(TAG, "[AuthNodeListener] Sending credentials again via next()");
                node.next(context, this);
                return;
            }

            // Fallback: sin manejo posible
            pluginState.reset();
            call.reject("Unhandled node state. Callbacks: " + callbackNames.toString());

        } catch (Exception e) {
            Log.d(TAG, "[AuthNodeListener] error catch (Exception e)");
            pluginState.reset();
            call.reject("Error processing node: " + e.getMessage(), e);
        }
    }
}
