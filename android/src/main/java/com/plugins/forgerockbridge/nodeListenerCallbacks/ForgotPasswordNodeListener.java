package com.plugins.forgerockbridge.nodeListenerCallbacks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.plugins.forgerockbridge.enums.ForgotPasswordEnum;
import com.plugins.forgerockbridge.state.PluginState;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.TextOutputCallback;
import org.forgerock.android.auth.callback.PasswordCallback;

import com.plugins.forgerockbridge.ErrorHandler;

public class ForgotPasswordNodeListener implements NodeListener<FRSession> {

    private static final String TAG = "ForgeRockBridge";
    private final PluginCall call;
    private final Context context;
    private final PluginState pluginState;
    private final ForgotPasswordEnum.IdPath idPath;

    public ForgotPasswordNodeListener(PluginCall call, Context context, PluginState pluginState, ForgotPasswordEnum.IdPath idPath) {
        this.call = call;
        this.context = context;
        this.pluginState = pluginState;
        this.idPath = idPath;
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
        FRSession currentSession = FRSession.getCurrentSession();
        if (currentSession != null) {
            Log.d(TAG, "[ForgotPasswordNodeListener:onSuccess] FRSession.logout()");
            currentSession.logout();
        }
        JSObject result = new JSObject();
        result.put("status", "success");
        result.put("message", "Password changed successfully");
        call.resolve(result);
    }

    @Override
    public void onCallbackReceived(Node node) {

        try {
            Log.d(TAG, "[ForgotPasswordNodeListener: onCallbackReceived]: call method onCallbackReceived");

            switch (idPath) {
                case INIT_FORGOT_PASS:
                    this.initForgotPasswordHandler(call, node);
                    break;
                case ANSWER_QUESTION:
                    this.answerQuestionHandler(call, node);
                    break;
                case CHANGE_PASS:
                    this.changePassHandler(call, node);
                    break;
                default:
                    JSObject out = new JSObject()
                    .put("status", "error")
                    .put("message", "Unhandled node state");
                    call.resolve(out);
            }


        } catch (Exception e) {
            Log.d(TAG, "[ForgotPasswordNodeListener: onCallbackReceived] error catch (Exception e)");
            call.reject("Error processing node: " + e.getMessage(), e);
        }

    }


    private void initForgotPasswordHandler(PluginCall call, Node node){

        final String username = call.getString("username", null);
        String errorMessage = null;

        for (Callback cb : node.getCallbacks()) {
            if (cb instanceof TextOutputCallback) {
                errorMessage = ((TextOutputCallback) cb).getMessage();
                Log.d(TAG, "[ForgotPasswordNodeListener: initForgotPasswordHandler] TextOutputCallback: "+ errorMessage);
                pluginState.setLastErrorMessage(errorMessage);

                JSObject result = new JSObject();
                result.put("status", "error");
                result.put("message", errorMessage);
                call.resolve(result);
                return;
            } else if (cb instanceof NameCallback) {
                Log.d(TAG, "[ForgotPasswordNodeListener: initForgotPasswordHandler] Has NameCallBack node");
                ((NameCallback) cb).setName(username);
                Log.d(TAG, "[ForgotPasswordNodeListener: initForgotPasswordHandler] Sending credentials again via next()");
                node.next(context, this);
                return;
            } else if (cb instanceof PasswordCallback) {
                Log.d(TAG, "[ForgotPasswordNodeListener: initForgotPasswordHandler] Has PasswordCallback node");
                pluginState.setPendingNode(node);
                JSObject out = new JSObject()
                .put("status", "success")
                .put("message", "verified username");
                call.resolve(out);
                return;
            }
        }

    }

    private void answerQuestionHandler (PluginCall call, Node node) {

        String errorMessage = null;
        boolean hasTextOutput = false;
        boolean hasConfirmationCallback = false;

        for (Callback cb : node.getCallbacks()) {
            if (cb instanceof TextOutputCallback) {
                errorMessage = ((TextOutputCallback) cb).getMessage();
                Log.d(TAG, "[ForgotPasswordNodeListener: answerQuestionHandler] TextOutputCallback: " + errorMessage);
                pluginState.setLastErrorMessage(errorMessage);
                hasTextOutput = true;
            } else if (cb instanceof ConfirmationCallback) {
                ((ConfirmationCallback) cb).setSelectedIndex(0);
                hasConfirmationCallback = true;
            }
        }

        if(hasTextOutput && hasConfirmationCallback) {
            node.next(context, new NodeListener<FRSession>() {
                @Override
                public void onCallbackReceived(@NonNull Node nextNode) {
                    Log.d(TAG, "[ForgotPasswordNodeListener: answerQuestionHandler] onCallbackReceived: ");
                    pluginState.setPendingNode(nextNode);
                    JSObject out = new JSObject()
                    .put("status", "error")
                    .put("message", pluginState.getLastErrorMessage());
                    call.resolve(out);
                }
                @Override
                public void onSuccess(@NonNull FRSession session) {
                    Log.d(TAG, "[ForgotPasswordNodeListener: answerQuestionHandler] onSuccess: ");
                    JSObject out = new JSObject()
                            .put("status", "success")
                            .put("message", "");
                    call.resolve(out);
                }

                @Override
                public void onException(@NonNull Exception e) {
                    for (Callback cb : node.getCallbacks()) {
                        if (cb instanceof TextOutputCallback) {
                            String errorMessage = ((TextOutputCallback) cb).getMessage();
                            Log.d(TAG, "[ForgotPasswordNodeListener: answerQuestionHandler: onException] TextOutputCallback: " + errorMessage);
                            pluginState.setLastErrorMessage(errorMessage);
                            JSObject out = new JSObject()
                            .put("status", "error")
                            .put("message", pluginState.getLastErrorMessage());
                            call.resolve(out);
                        }
                    }
                }
            });
            return;
        }

        pluginState.setPendingNode(node);
        JSObject out = new JSObject()
        .put("status", "success")
        .put("message", "question success");
        call.resolve(out);
    }

    private void changePassHandler (PluginCall call, Node node) {

        boolean hasTextOutput = false;
        boolean hasConfirmationCallback = false;

        for (Callback cb : node.getCallbacks()) {
            if (cb instanceof TextOutputCallback) {
                pluginState.setLastErrorMessage(((TextOutputCallback) cb).getMessage());
                Log.d(TAG, "[ForgotPasswordNodeListener: changePassHandler] TextOutputCallback: " + pluginState.getLastErrorMessage());
                hasTextOutput = true;
            } else if (cb instanceof ConfirmationCallback) {
                ((ConfirmationCallback) cb).setSelectedIndex(0);
                hasConfirmationCallback = true;
            }

        }

        if(hasTextOutput && hasConfirmationCallback) {

            node.next(context, new NodeListener<FRSession>() {
                @Override
                public void onCallbackReceived(@NonNull Node nextNode) {
                    Log.d(TAG, "[ForgotPasswordNodeListener: changePassHandler] onCallbackReceived: ");
                    pluginState.setPendingNode(nextNode);
                    JSObject out = new JSObject()
                            .put("status", "error")
                            .put("message", pluginState.getLastErrorMessage());
                    call.resolve(out);
                }
                @Override
                public void onSuccess(@NonNull FRSession session) {
                    Log.d(TAG, "[ForgotPasswordNodeListener: changePassHandler] onSuccess: ");
                    JSObject out = new JSObject()
                    .put("status", "success")
                    .put("message", "Password changed successfully");
                    call.resolve(out);
                }

                @Override
                public void onException(@NonNull Exception e) {
                    Log.d(TAG, "[ForgotPasswordNodeListener: changePassHandler] onException: ");
                }
            });

        }


    }

}
