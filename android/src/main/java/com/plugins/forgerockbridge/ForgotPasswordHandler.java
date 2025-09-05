package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
import org.forgerock.android.auth.UserInfo;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.exception.AuthenticatorException;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.HiddenValueCallback;
import org.forgerock.android.auth.callback.TextOutputCallback;

import org.json.JSONObject;

import com.plugins.forgerockbridge.ErrorHandler.FRException;
import com.plugins.forgerockbridge.nodeListenerCallbacks.ForgotPasswordNodeListener;
import com.plugins.forgerockbridge.state.PluginState;


import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ForgotPasswordHandler {
    private static final String TAG = "ForgeRockBridge";

    public static void startForgotPasswordJourney(PluginCall call, Context context, NodeListener<FRSession> listener ) {
        Log.d(TAG, "[ForgotPasswordHandler: startForgotPasswordJourney ] call method startForgotPasswordJourney");
        String journey = call.getString("journey");
        String username = call.getString("username");

        if (journey == null) {
            Log.e(TAG, "[ForgotPasswordHandler: startForgotPasswordJourney ]: Error in get Journey");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_JOURNEY);
            return;
        }

         if (username == null) {
            Log.e(TAG, "[ForgotPasswordHandler: startForgotPasswordJourney]: Error in get the username");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_JOURNEY);
            return;
        }

        FRSession.authenticate(context, journey, listener);
    }

    public static void getSecurityQuestion(PluginCall call, PluginState state) {
        Node pending = state.getPendingNode();
        String question = null;

        if (pending == null) {
            Log.e(TAG, "[ForgotPasswordHandler: getSecurityQuestion]: NO PENDING NODE SAVE");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.NO_PENDING_NODE, "NO PENDING NODE SAVE");
            return;
        }


        for (Callback cb : pending.getCallbacks()) {
            if (cb instanceof PasswordCallback) {
                question = ((PasswordCallback) cb).getPrompt();
                break;
            }
        }

        if (question == null) {
            Log.e(TAG, "[ForgotPasswordHandler: getSecurityQuestion]: NO QUESTION FOUND");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.NO_QUESTION_FOUND, "NO QUESTION FOUND");
            return;
        }

        JSObject out = new JSObject()
            .put("question", question);
            Log.d(TAG, "[ForgotPasswordHandler: getSecurityQuestion] Sending question");
        call.resolve(out);
    }

    public static void answerQuestionForgotPassword(PluginCall call, PluginState state, Context context ) {
        Node pending = state.getPendingNode();
        String answer = call.getString("answer");

        if (pending == null) {
            Log.e(TAG, "[ForgotPasswordHandler: answerQuestionForgotPassword]: NO PENDING NODE SAVE");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.NO_PENDING_NODE, "NO PENDING NODE SAVE");
            return;
        }

        if (answer == null || answer.trim().isEmpty()) {
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_PARAMETER, "Missing answer");
            return;
        }

        Log.d(TAG, "[ForgotPasswordHandler: answerQuestionForgotPassword] answer: " + answer);

        for (Callback cb : pending.getCallbacks()) {
            if (cb instanceof PasswordCallback) {
                ((PasswordCallback) cb).setPassword(answer.toCharArray());
                break;
            } 
        }

        pending.next(context, new ForgotPasswordNodeListener(call, context, state));

    }

    public static void changePasswordForgotPassword (PluginCall call, PluginState state, Context context ) {

    }


}