package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;


import org.forgerock.android.auth.FRSession;

import org.forgerock.android.auth.NodeListener;

import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.callback.ValidatedPasswordCallback;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.callback.Callback;

import com.plugins.forgerockbridge.nodeListenerCallbacks.ForgotPasswordNodeListener;
import com.plugins.forgerockbridge.state.PluginState;

public class ForgotPasswordHandler {
    private static final String TAG = "ForgeRockBridge";

    public static void startForgotPasswordJourney(PluginCall call, Context context, NodeListener<FRSession> listener ) {
        Log.d(TAG, "[ForgotPasswordHandler: startForgotPasswordJourney ] call method startForgotPasswordJourney");


        String journey = call.getString("journey");
        String username = call.getString("username");

        if (journey == null || journey.trim().isEmpty()) {
            Log.e(TAG, "[ForgotPasswordHandler: startForgotPasswordJourney ]: Error in get Journey");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_JOURNEY);
            return;
        }

         if (username == null || username.trim().isEmpty()) {
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

        if (question == null || question.trim().isEmpty()) {
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
            Log.e(TAG, "[ForgotPasswordHandler: answerQuestionForgotPassword]: MISSING ANSWER");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_PARAMETER, "Missing answer");
            return;
        }


        for (Callback cb : pending.getCallbacks()) {
            if (cb instanceof PasswordCallback) {
                ((PasswordCallback) cb).setPassword(answer.toCharArray());
                break;
            } 
        }

        pending.next(context, new ForgotPasswordNodeListener(call, context, state));
    }

    public static void changePasswordForgotPassword (PluginCall call, PluginState state, Context context ) {
        Log.d(TAG, "[ForgotPasswordHandler: changePasswordForgotPassword]: CALL METHOD changePasswordForgotPassword");
        Node pending = state.getPendingNode();
        String password = call.getString("password", null);

        if (pending == null) {
            Log.e(TAG, "[ForgotPasswordHandler: changePasswordForgotPassword]: NO PENDING NODE SAVE");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.NO_PENDING_NODE, "NO PENDING NODE SAVE");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            Log.e(TAG, "[ForgotPasswordHandler: changePasswordForgotPassword]: MISSING PASSWORD");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_PARAMETER, "Missing password");
            return;
        }

        for (Callback cb : pending.getCallbacks()) {
            if (cb instanceof ValidatedPasswordCallback) {
                ((ValidatedPasswordCallback) cb).setPassword(password.toCharArray());
                break;
            }
        }

        pending.next(context, new ForgotPasswordNodeListener(call, context, state));
    }

}