package com.plugins.forgerockbridge;

import android.content.Context;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.plugins.forgerockbridge.enums.ForgotPasswordEnum;
import com.plugins.forgerockbridge.nodeListenerCallbacks.ForgotPasswordNodeListener;
import com.plugins.forgerockbridge.state.PluginState;
import com.plugins.forgerockbridge.nodeListenerCallbacks.OTPNodeListener;
import com.plugins.forgerockbridge.nodeListenerCallbacks.OTPDeleteNodeListener;

@CapacitorPlugin(name = "ForgerockBridge")
public class ForgerockBridgePlugin extends Plugin {

    public static Context context;
    private final PluginState pluginState = new PluginState();


    @Override
    public void load() {
        super.load();
        context = getContext();
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        SDKInitializer.handle(call, context);
    }

    @PluginMethod
    public void authenticate(PluginCall call) {
        AuthenticationHandler.handle(call, getContext(), pluginState);
    }

    @PluginMethod
    public void userInfo(PluginCall call) {
        UserHandler.getUserInfo(call);
    }

    @PluginMethod
    public void logout(PluginCall call) {
        UserHandler.logout(call);
    }

    @PluginMethod
    public void initializeOTPRegister(PluginCall call) {
        OTPTokenHandler.startOtpJourney(call, context, new OTPNodeListener(call, context, pluginState));
    }

    @PluginMethod
    public void deleteOTPRegister(PluginCall call) {
        OTPTokenHandler.startOtpJourney(call, context, new OTPDeleteNodeListener(call, context, pluginState));
    }

    @PluginMethod
    public void hasRegisteredMechanism(PluginCall call) {
        OTPTokenHandler.hasRegisteredMechanism(call, context);
    }

    @PluginMethod
    public void validateOTP(PluginCall call) {
        OTPTokenHandler.validateExistenceOTP(call, context);
    }

    @PluginMethod
    public void generateOTP(PluginCall call) {
        OTPTokenHandler.generateOTP(call, context);
    }

    @PluginMethod
    public void initForgotPassword(PluginCall call) {
        pluginState.reset();
        ForgotPasswordHandler.startForgotPasswordJourney(call, context, new ForgotPasswordNodeListener(call, context, pluginState, ForgotPasswordEnum.IdPath.INIT_FORGOT_PASS));
    }

    @PluginMethod
    public void getQuestionForgotPassword(PluginCall call) {
        ForgotPasswordHandler.getSecurityQuestion(call, pluginState);
    }

    @PluginMethod
    public void answerQuestionForgotPassword(PluginCall call) {
        ForgotPasswordHandler.answerQuestionForgotPassword(
            call,
            pluginState,
            context
        );
    }

    @PluginMethod
    public void changePasswordForgotPassword(PluginCall call) {
        ForgotPasswordHandler.changePasswordForgotPassword(
                call,
                pluginState,
                context
        );
    }

}
