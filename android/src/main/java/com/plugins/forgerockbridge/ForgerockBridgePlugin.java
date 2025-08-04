package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
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
    public void initializeDeleteOTP(PluginCall call) {
        Log.d("ForgeRockBridge","Primer llamado");
        OTPTokenHandler.startOtpJourney(call, context, new OTPNodeListener(call, context, pluginState));
    }

    @PluginMethod
    public void deleteOTPRegister(PluginCall call) {
        OTPTokenHandler.startOtpJourney(call, context, new OTPDeleteNodeListener(call, context, pluginState));
    }

    @PluginMethod
    public void validateOTP(PluginCall call) {
        OTPTokenHandler.validateOTP(call, context);
    }


    
}
