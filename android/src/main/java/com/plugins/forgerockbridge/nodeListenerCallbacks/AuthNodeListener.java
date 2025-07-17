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
        Log.d(TAG, "[AuthNodeListener] instantiated for call with isRetry=" + call.getBoolean("isRetry"));
    }

    @Override
    public void onSuccess(FRSession session) {
        try {
            SSOToken token = session.getSessionToken();
            JSObject result = new JSObject()
                    .put("status", "authenticated")
                    .put("token", token.getValue());
            Log.d(TAG, "[AuthNodeListener] << RESOLVE onSuccess >> " + result.toString());
            call.resolve(result);
            pluginState.reset();
        } catch (Exception e) {
            Log.e(TAG, "[AuthNodeListener] Failed to retrieve token after successful authentication: " + e.getMessage(), e);
            call.reject("Failed to retrieve token: " + e.getMessage(), e);
        }
    }

    @Override
    public void onException(Exception e) {
        Log.e(TAG, "[AuthNodeListener] << onException >> " + e.getMessage(), e);
        pluginState.reset();
        call.reject("Authentication failed: " + e.getMessage(), e);
    }

    @Override
    public void onCallbackReceived(Node node) {
        try {
            // 1) Log incoming call data
            Log.d(TAG, "[AuthNodeListener] << CALL DATA >> " + call.getData().toString());


            // 2) Parse inputs
            final String username = call.getString("username");
            final String password = call.getString("password");
            final boolean isRetry = Boolean.TRUE.equals(call.getBoolean("isRetry"));
            Log.d(TAG, "[AuthNodeListener] Parsed input → username='" + username + "', password='" + password + "', isRetry=" + isRetry);

            // 3) Determine which node to act on
            Node activeNode = pluginState.getPendingNode() != null ? pluginState.getPendingNode() : node;

            // 4) Inspect callbacks
            boolean hasErrorMessage = false;
            boolean hasConfirmation = false;
            String errorMessage = null;
            JSONArray callbackNames = new JSONArray();

            for (Callback cb : activeNode.getCallbacks()) {
                String cbName = cb.getClass().getSimpleName();
                callbackNames.put(cbName);
                Log.d(TAG, "[AuthNodeListener] Callback onCallbackReceived: " + cbName);
                if (cb instanceof TextOutputCallback) {
                    errorMessage = ((TextOutputCallback) cb).getMessage();
                    Log.d(TAG, "[AuthNodeListener] TextOutputCallback message: " + errorMessage);
                    pluginState.setLastErrorMessage(errorMessage);
                    hasErrorMessage = true;
                } else if (cb instanceof ConfirmationCallback) {
                    hasConfirmation = true;
                    if (isRetry) {
                        ((ConfirmationCallback) cb).setSelectedIndex(0);
                        Log.d(TAG, "[AuthNodeListener] ConfirmationCallback set to OK (index 0)");
                    }
                }
            }

            // 5) FIRST attempt: handle FRE015 / FRE016
            if (hasErrorMessage && hasConfirmation && errorMessage != null) {
                if (errorMessage.contains("FRE016")) {
                    JSObject out = new JSObject()
                            .put("status", "failure")
                            .put("errorMessage", "FRE016")
                            .put("callbacks", callbackNames);
                    Log.d(TAG, "[AuthNodeListener] << RESOLVE FRE016 >> " + out.toString());
                    call.resolve(out);
                    pluginState.reset();
                    return;
                } else if (errorMessage.contains("FRE015")) {
                    pluginState.setPendingNode(activeNode);
                    pluginState.setDidSubmitConfirmation(false);
                    JSObject out = new JSObject()
                            .put("status", "awaitingRetry")
                            .put("errorMessage", errorMessage)
                            .put("callbacks", callbackNames);
                    Log.d(TAG, "[AuthNodeListener] << RESOLVE FRE015 (awaitingRetry) >> " + out.toString());
                    call.resolve(out);
                    return;
                }
            }

            // 6) SECOND attempt: send ConfirmationCallback back to ForgeRock
            if (isRetry && hasConfirmation && !pluginState.getDidSubmitConfirmation()) {
                pluginState.setDidSubmitConfirmation(true);
                Log.d(TAG, "[AuthNodeListener] << SECOND ATTEMPT >> Confirmation OK, forwarding to ForgeRock…");

                Log.d(TAG, "[AuthNodeListener] SECOND attempt About to call node.next with pending node. Callbacks:");
                for (Callback cb : pluginState.getPendingNode().getCallbacks()) {
                    Log.d(TAG, "[AuthNodeListener] SECOND attempt PendingNode callback before next: " + cb.getClass().getSimpleName());
                }
                activeNode.next(context, new NodeListener<FRSession>() {
                    @Override
                    public void onCallbackReceived(Node nextNode) {
                        boolean hasConfirmation = false;
                        JSONArray callbackNames = new JSONArray();

                        for (Callback cb : nextNode.getCallbacks()) {
                            String cbName = cb.getClass().getSimpleName();
                            callbackNames.put(cbName);
                            if (cb instanceof ConfirmationCallback) {
                                hasConfirmation = true;
                            }
                        }

                        if (hasConfirmation) {
                            pluginState.setPendingNode(nextNode);
                            pluginState.setDidSubmitConfirmation(true);
                        } else {
                            pluginState.setPendingNode(null);
                            pluginState.setDidSubmitConfirmation(true);
                        }

                        JSObject out = new JSObject()
                                .put("status", "awaitingRetry")
                                .put("errorMessage", pluginState.getLastErrorMessage())
                                .put("callbacks", callbackNames);

                        Log.d(TAG, "[AuthNodeListener] << RESOLVE awaitingRetry after next() >> " + out.toString());
                        call.resolve(out);
                    }

                    @Override
                    public void onSuccess(FRSession session) {
                        pluginState.reset();
                        JSObject ok = new JSObject()
                                .put("status", "authenticated")
                                .put("token", session.getSessionToken().getValue());
                        Log.d(TAG, "[AuthNodeListener] << RESOLVE onSuccess after confirmation >> " + ok.toString());
                        call.resolve(ok);
                    }

                    @Override
                    public void onException(Exception ex) {
                        pluginState.reset();
                        Log.e(TAG, "[AuthNodeListener] << ERROR after confirmation >> " + ex.getMessage(), ex);
                        call.reject("Authentication failed after confirmation: " + ex.getMessage(), ex);
                    }
                });
                return;
            }

            // 7) THIRD attempt: supply credentials again
            boolean hasName = false, hasPass = false;
            for (Callback cb : activeNode.getCallbacks()) {
                if (cb instanceof NameCallback) hasName = true;
                if (cb instanceof PasswordCallback) hasPass = true;
            }
            if (hasName && hasPass) {
                continueWithLogin(activeNode, username, password);
                return;
            }

            // 8) Fallback
            Log.w(TAG, "[AuthNodeListener] Unexpected node state, fallback to awaitingRetry or reject.");
            if (pluginState.getPendingNode() != null) {
                JSObject fallback = new JSObject()
                        .put("status", "awaitingRetry")
                        .put("errorMessage", errorMessage != null ? errorMessage : "Unexpected flow")
                        .put("callbacks", callbackNames);
                Log.d(TAG, "[AuthNodeListener] << RESOLVE fallback >> " + fallback.toString());
                call.resolve(fallback);
            } else {
                call.reject("Unexpected authentication result: No user nor next node.");
            }

        } catch (Exception e) {
            Log.e(TAG, "[AuthNodeListener] Error processing node: " + e.getMessage(), e);
            call.reject("Error processing node: " + e.getMessage(), e);
        }
    }

    private void continueWithLogin(Node node, String username, String password) {
        for (Callback cb : node.getCallbacks()) {
            String n = cb.getClass().getSimpleName();
            Log.d(TAG, "[AuthNodeListener] Callback continueWithLogin: " + n);
            if (cb instanceof NameCallback) {
                ((NameCallback) cb).setName(username);
                Log.d(TAG, "[AuthNodeListener] Setting username: " + username);
            } else if (cb instanceof PasswordCallback) {
                ((PasswordCallback) cb).setPassword(password.toCharArray());
                Log.d(TAG, "[AuthNodeListener] Setting password for user.");
            }
        }

        Log.d(TAG, "[AuthNodeListener] About to call node.next with current node. Callbacks:");
        for (Callback cb : node.getCallbacks()) {
            Log.d(TAG, "[AuthNodeListener] Callback before next: " + cb.getClass().getSimpleName());
        }

        // Ya estás en una instancia de AuthNodeListener. Usa `this`:
        node.next(context, this);
        Log.d(TAG, "[AuthNodeListener] Called node.next() with credentials.");
    }

}