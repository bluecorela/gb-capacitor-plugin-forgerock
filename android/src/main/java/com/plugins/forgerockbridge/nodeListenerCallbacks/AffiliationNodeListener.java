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
import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.callback.HiddenValueCallback;
import org.forgerock.android.auth.callback.KbaCreateCallback;
import org.forgerock.android.auth.callback.StringAttributeInputCallback;
import org.forgerock.android.auth.callback.TextOutputCallback;
import org.forgerock.android.auth.callback.ValidatedPasswordCallback;
import org.forgerock.android.auth.callback.ValidatedUsernameCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class AffiliationNodeListener implements NodeListener<FRSession> {
    private static final String TAG = "ForgeRockBridge";
    private final PluginCall call;
    private final Context context;
    private final PluginState pluginState;

    public AffiliationNodeListener(PluginCall call, Context context, PluginState pluginState) {
        this.call = call;
        this.context = context;
        this.pluginState = pluginState;
    }

    @Override
    public void onCallbackReceived(@NonNull Node node) {
        String Step = call.getString("step");
        String metaData = call.getString("meta");

        boolean pendingUser = false;
        boolean pendingPass = false;
        boolean hasKBA = false;

        String email = "";

        JSONArray callbackNames = new JSONArray();
        JSONArray securityQuestions = new JSONArray();
        for (Callback cb : node.getCallbacks()) {
            callbackNames.put(cb.getClass().getSimpleName());

            if (cb instanceof StringAttributeInputCallback stringCallback) {
                if(Objects.equals(Step, "PERSONAL_ID")){
                    stringCallback.setValue(metaData);
                    node.next(context, this);
                }else if(Objects.equals(Step, "USERNAME_PASSWORD")){
                    Log.d(TAG, "[AffiliationNodeListener] stringCallback " + ((StringAttributeInputCallback) cb).getContent());
                    saveNodePending(node);
                    sendResolve("next", "");
                }
            } else if(cb instanceof HiddenValueCallback) {

                if ("mail".equalsIgnoreCase(((HiddenValueCallback) cb).getId()) && Objects.equals(Step, "PERSONAL_ID")) {
                    email = ((HiddenValueCallback) cb).getValue();
                    saveNodePending(node);
                    sendResolve("next", email);
                }
                Log.d(TAG, "[AffiliationNodeListener] Send email " + email);
            }else if(cb instanceof ConfirmationCallback) {
                if(((ConfirmationCallback) cb).getMessageType() == 0){

                    if(!Objects.equals(Step, "PERSONAL_ID")){
                        saveNodePending(node);
                        sendResolve("next", "");
                    }
                }

            }else if(cb instanceof ValidatedUsernameCallback) {
                pendingUser = true;
                Log.d(TAG, "[AffiliationNodeListener] ValidatedUsernameCallback " + ((ValidatedUsernameCallback) cb).getContent());
            }else if(cb instanceof ValidatedPasswordCallback) {
                pendingPass = true;
                Log.d(TAG, "[AffiliationNodeListener] ValidatedPasswordCallback " + ((ValidatedPasswordCallback) cb).getContent());
            }else if(cb instanceof KbaCreateCallback && securityQuestions.length() == 0) {
                List<String> raw = ((KbaCreateCallback) cb).getPredefinedQuestions();

                for (String q : raw) {
                    JSONObject finalQuestion = new JSONObject();
                    try {
                        finalQuestion.put("value", q);
                        finalQuestion.put("label", q);
                        securityQuestions.put(finalQuestion);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                hasKBA = true;
                Log.d(TAG, "[AffiliationNodeListener] KbaCreateCallback " + ((KbaCreateCallback) cb).getPredefinedQuestions());
            }
        }

        if(Objects.equals(Step, "OTP") && pendingUser && pendingPass){
            saveNodePending(node);
            sendResolve("next", "");
        }
        if(hasKBA){
            Log.d(TAG, "[AffiliationNodeListener] sequrityQuestions " + securityQuestions.toString());
            sendSecurityQuestions(node, securityQuestions);
        }
    }

    @Override
    public void onSuccess(FRSession frSession) {
        Log.d(TAG, "onSuccess: " );
        sendResolve("success", "");
    }

    @Override
    public void onException(@NonNull Exception e) {
        Log.d(TAG, "ERROR: "+e.getMessage() );
        sendResolve("error", e.getMessage());
    }

    private void sendResolve(String status, String message){
        Log.d(TAG, "[AffiliationNodeListener]: sendResolve");

        JSObject result = new JSObject()
                .put("status", status)
                .put("message", message);

        call.resolve(result);
    }

    private void sendSecurityQuestions(Node node, JSONArray sequrityQuestions){
        Log.d(TAG, "[AffiliationNodeListener]: sendSecurityQuestions");
        this.saveNodePending(node);

        JSObject result = new JSObject()
                .put("status", "next")
                .put("data", sequrityQuestions);

        call.resolve(result);
    }

    private void saveNodePending(Node node){
        pluginState.setPendingNode(node);
    }
}
