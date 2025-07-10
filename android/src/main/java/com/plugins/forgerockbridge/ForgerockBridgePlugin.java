package com.plugins.forgerockbridge;

import android.content.Context;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.plugins.forgerockbridge.nodeListenerCallbacks.AuthNodeListener;


@CapacitorPlugin(name = "ForgerockBridge")
public class ForgerockBridgePlugin extends Plugin {

    public static Context context;

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
        AuthenticationHandler.handle(call, context);
    }

    @PluginMethod
    public void userInfo(PluginCall call) {
        UserHandler.getUserInfo(call);
    }

    @PluginMethod
    public void logout(PluginCall call) {
        UserHandler.logout(new AuthNodeListener(call, getContext()));
    }
}
