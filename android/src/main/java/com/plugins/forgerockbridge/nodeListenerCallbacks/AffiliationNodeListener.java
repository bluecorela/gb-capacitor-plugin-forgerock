package com.plugins.forgerockbridge.nodeListenerCallbacks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.plugins.forgerockbridge.ErrorHandler;
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

import com.plugins.forgerockbridge.ErrorHandler.FRException;

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

        TextOutputCallback textOutputCallback = null;
        ConfirmationCallback confirmationCallback = null;
        HiddenValueCallback hiddenValueCallback = null;

        boolean pendingUser = false;
        boolean pendingPass = false;
        boolean hasKBA = false;
        boolean hasConfirmCb = false;
        boolean isError = false;

        String email = "";

        JSONArray securityQuestions = new JSONArray();

        for (Callback cb : node.getCallbacks()) {

            if (cb instanceof StringAttributeInputCallback stringCallback) {
              handleStringAttributeInput(stringCallback, node, metaData, Step);
            } else if(cb instanceof TextOutputCallback ) {
                textOutputCallback = (TextOutputCallback) cb;
               if( textOutputCallback.getMessageType() == 2){
                   isError = true;
               }
            }else if(cb instanceof ConfirmationCallback) {
                hasConfirmCb = true;
                confirmationCallback = (ConfirmationCallback) cb;
            }else if(cb instanceof HiddenValueCallback) {
                hiddenValueCallback = (HiddenValueCallback) cb;
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


        //Actions segment
        if(textOutputCallback != null){
            Log.d(TAG, "[AffiliationNodeListener] Texoutput content " + textOutputCallback.getContent());
            handleTextOutPut(textOutputCallback, node);
        }

        if(hasConfirmCb && confirmationCallback != null && !isError ){
            if(confirmationCallback.getMessageType() == 0){
                if(!Objects.equals(Step, "PERSONAL_ID")){
                    saveNodePending(node);
                    sendResolve("next", "");
                }
            }
        }

        if(hiddenValueCallback != null){
            if ("mail".equalsIgnoreCase(hiddenValueCallback.getId())) {
                assert Step != null;
                if (Step.equals("PERSONAL_ID") || Step.equals("RESEND")) {
                    Log.d(TAG, "[AffiliationNodeListener] se envio " + Step);
                    email = hiddenValueCallback.getValue();
                    saveNodePending(node);
                    sendResolve("next", email);
                }
            }
        }

        if(pendingUser && pendingPass){
            handleUserPassCallback(node, Step);
        }

        if(hasKBA){
            Log.d(TAG, "[AffiliationNodeListener] sequrityQuestions " + securityQuestions.toString());
            sendSecurityQuestions(node, securityQuestions);
        }


    }

    @Override
    public void onSuccess(FRSession frSession) {
        Log.d(TAG, "AffiliationNodeListener] onSuccess: " );
        sendResolve("success", "");
    }

    @Override
    public void onException(@NonNull Exception e) {
        Log.e(TAG, "AffiliationNodeListener] ERROR: "+e.getMessage() );
        sendResolve("error", e.getMessage());
    }

    private void sendResolve(String status, String message){
        Log.d(TAG, "[AffiliationNodeListener]: sendResolve");

        JSObject result = new JSObject()
                .put("status", status)
                .put("message", message);
        Log.d(TAG, "[AffiliationNodeListener]: result"+result);
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

    private void handleTextOutPut(TextOutputCallback textOutputCallback, Node node){
        if(textOutputCallback.getMessageType() == 2){
           saveNodePending(node);
           sendResolve("error", textOutputCallback.getMessage());
        }
        Log.d(TAG, "[AffiliationNodeListener] TextOutputCallback " + textOutputCallback.getContent());
    }

    private void handleStringAttributeInput(StringAttributeInputCallback stringCallback, Node node, String metaData, String Step){
        if(Objects.equals(Step, "PERSONAL_ID")){
            stringCallback.setValue(metaData);
            node.next(context, this);
        }else if(Objects.equals(Step, "USERNAME_PASS")){
            Log.d(TAG, "[AffiliationNodeListener] stringCallback " + stringCallback.getContent());
            saveNodePending(node);
            sendResolve("next", "");
        }
    }

    private void handleUserPassCallback(Node node, String step){
        String message = "";
        String status = "next";
        saveNodePending(node);
        if(Objects.equals(step, "USERNAME_PASS") ){
            message = "Invalid User ";
            status = "retry";
        }
        Log.d(TAG, "[AffiliationNodeListener] handleUserPassCallback " +status+ "message" + message);
        sendResolve(status, message);
    }

    private void handle
}
