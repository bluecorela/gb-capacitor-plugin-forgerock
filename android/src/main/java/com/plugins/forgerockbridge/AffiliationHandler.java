package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.plugins.forgerockbridge.interceptor.ForgotPasswordInterceptor;
import com.plugins.forgerockbridge.nodeListenerCallbacks.AffiliationNodeListener;
import com.plugins.forgerockbridge.nodeListenerCallbacks.AuthNodeListener;
import com.plugins.forgerockbridge.state.PluginState;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.RequestInterceptorRegistry;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.callback.HiddenValueCallback;
import org.forgerock.android.auth.callback.KbaCreateCallback;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.StringAttributeInputCallback;
import org.forgerock.android.auth.callback.ValidatedPasswordCallback;
import org.forgerock.android.auth.callback.ValidatedUsernameCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class AffiliationHandler {
    private static final String TAG = "ForgeRockBridge";

    public static void StepRouter(PluginCall call, Context context, PluginState pluginState) throws JSONException {
        String journey = call.getString("journey");
        String step = call.getString("step");

        Log.d(TAG, "[AffiliationHandler]: STEP "+ step);
        if (journey == null) {
            Log.e(TAG, "[AffiliationHandler]: Error in get Journey");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_JOURNEY);
            return;
        }

        if (step == null) {
            Log.e(TAG, "[AffiliationHandler]: Error in get step");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_JOURNEY);
            return;
        }

        switch (step){
            case "PERSONAL_ID":
                startJourney(call, context, pluginState, journey);
                break;
            case "OTP":
                confirmEmail(call, context, pluginState);
                break;
            case "USERNAME_PASS":
                setUsernamePassword(call, context, pluginState);
                break;
            case "AVATAR":
                setAvatar(call, context, pluginState);
                break;
            case "KBA":
                setKBA(call, context, pluginState);
                break;
            case "TERMS":
                setTerms(call, context, pluginState);
                break;
            case "RESEND":
                resendEmail(call, context, pluginState);
                break;
        }
    }

    public static void startJourney(PluginCall call, Context context, PluginState pluginState, String journey){
        String language = call.getString("language");

        if (language != null) {
            Log.d(TAG, "[AffiliationHandler: GET LANGUAGE");
            RequestInterceptorRegistry.getInstance()
                .register(new ForgotPasswordInterceptor(language));
        }

        FRSession.authenticate(context, journey, new AffiliationNodeListener(call, context, pluginState));
    }

    public static void resendEmail(PluginCall call, Context context, PluginState pluginState) {
        Node pending = pluginState.getPendingNode();
        String Step = call.getString("step");
        JSONArray callbackNames = new JSONArray();

        for (Callback cb : pending.getCallbacks()) {
            callbackNames.put(cb.getClass().getSimpleName());
            if(cb instanceof ConfirmationCallback confirmationCallback) {
                confirmationCallback.setSelectedIndex(1);
            }

        }
        pending.next(context, new AffiliationNodeListener(call, context, pluginState));
        Log.d(TAG, "[AffiliationHandler] Callbacks Resend: " + callbackNames.toString());
    }

    public static void confirmEmail(PluginCall call, Context context, PluginState pluginState){
        Node pending = pluginState.getPendingNode();

        JSONArray callbackNames = new JSONArray();

        for (Callback cb : pending.getCallbacks()) {
            callbackNames.put(cb.getClass().getSimpleName());
            if (cb instanceof NameCallback) {
                String metaData = call.getString("meta");

                NameCallback nameCallback = (NameCallback) cb;

                nameCallback.setName(metaData);

                pending.next(context, new AffiliationNodeListener(call, context, pluginState));
            }
        }
        Log.d(TAG, "[AffiliationHandler] Callbacks Here: " + callbackNames.toString());
    }

    public static void setUsernamePassword(PluginCall call, Context context, PluginState pluginState){
        Node pending = pluginState.getPendingNode();

        JSObject metaData = call.getObject("meta");
        String username = metaData.getString("user");
        String password = metaData.getString("password");

        JSONArray callbackNames = new JSONArray();
        Log.d(TAG, "[AffiliationHandler] Data "+metaData);
        Log.d(TAG, "[AffiliationHandler] User "+username);
        Log.d(TAG, "[AffiliationHandler] Pass "+password +" Change "+ password.toCharArray());

        for (Callback cb : pending.getCallbacks()) {
            callbackNames.put(cb.getClass().getSimpleName());

            if(cb instanceof ValidatedUsernameCallback UserName) {
                UserName.setUsername(username);
                Log.d(TAG, "[AffiliationHandler] ValidatedUsernameCallback " + ((ValidatedUsernameCallback) cb).getContent());
            }else if(cb instanceof ValidatedPasswordCallback UserPassword) {
                UserPassword.setPassword(password.toCharArray());
                Log.d(TAG, "[AffiliationHandler] ValidatedPasswordCallback " + ((ValidatedPasswordCallback) cb).getContent());
            }
        }

        pending.next(context, new AffiliationNodeListener(call, context, pluginState));

        Log.d(TAG, "[AffiliationHandler] Callbacks USERPASS: " + callbackNames.toString());
    }

    public static void setAvatar(PluginCall call, Context context, PluginState pluginState){
        Node pending = pluginState.getPendingNode();

        JSONArray callbackNames = new JSONArray();

        for (Callback cb : pending.getCallbacks()) {
            callbackNames.put(cb.getClass().getSimpleName());
            if (cb instanceof StringAttributeInputCallback nameCallback) {
                String metaData = call.getString("meta");

                Log.d(TAG, "[AffiliationHandler] avatar: " + metaData);

                nameCallback.setValue(metaData);

                pending.next(context, new AffiliationNodeListener(call, context, pluginState));
            }
        }
        Log.d(TAG, "[AffiliationHandler] Callbacks AVATAR: " + callbackNames.toString());
    }

    public static void setKBA(PluginCall call, Context context, PluginState pluginState) throws JSONException {
        Node pending = pluginState.getPendingNode();

        JSONArray callbackNames = new JSONArray();
        JSONArray metaData = call.getArray("meta");
        if (metaData == null || metaData.length() == 0) {
            Log.e(TAG, "[AffiliationHandler]: its empty");
            return;
        }

        JSONObject obj = metaData.getJSONObject(0);
        JSONArray questionsValues = obj.optJSONArray("questionsValues");
        JSONArray answersValues = obj.optJSONArray("answersValues");

        int kbaIndex = 0;

        Log.d(TAG, "[AffiliationHandler] ARRAY: " + metaData);
        Log.d(TAG, "[AffiliationHandler] questionsValues: " + questionsValues);
        Log.d(TAG, "[AffiliationHandler] answersValues: " + answersValues);

        for (Callback cb : pending.getCallbacks()) {
            callbackNames.put(cb.getClass().getSimpleName());
            if (cb instanceof KbaCreateCallback) {
                KbaCreateCallback kbaCallback = (KbaCreateCallback) cb;

                if (kbaIndex >= questionsValues.length() || kbaIndex >= answersValues.length()) {
                    Log.d(TAG, "[AffiliationHandler] empty Q/A para KBA index=" + kbaIndex);
                    kbaIndex++;
                    continue;
                }

                String question = questionsValues.optString(kbaIndex, "").trim();
                String answer   = answersValues.optString(kbaIndex, "").trim();

                if (question.isEmpty() || answer.isEmpty()) {
                    Log.w(TAG, "[AffiliationHandler] Q/A empty to index=" + kbaIndex);
                    kbaIndex++;
                    continue;
                }
                kbaCallback.setSelectedQuestion(question);
                kbaCallback.setSelectedAnswer(answer);

                Log.d(TAG, "[AffiliationHandler]view content" +  cb.getContent());

                kbaIndex++;
            }
        }
        pending.next(context, new AffiliationNodeListener(call, context, pluginState));
        Log.d(TAG, "[AffiliationHandler] Callbacks KBA: " + callbackNames.toString());
    }

    public static void setTerms(PluginCall call, Context context, PluginState pluginState){
        Node pending = pluginState.getPendingNode();
        JSONArray callbackNames = new JSONArray();

        for (Callback cb : pending.getCallbacks()) {
            callbackNames.put(cb.getClass().getSimpleName());
            if (cb instanceof ConfirmationCallback confirmCallback) {
                String metaData = call.getString("meta");

                Log.d(TAG, "[AffiliationHandler] TERMS: " + metaData);

                confirmCallback.setSelectedIndex(0);

                pending.next(context, new AffiliationNodeListener(call, context, pluginState));
            }
        }
        Log.d(TAG, "[AffiliationHandler] Callbacks TERMS: " + callbackNames.toString());
    }
}
