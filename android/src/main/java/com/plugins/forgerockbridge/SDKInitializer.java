package com.plugins.forgerockbridge;

import android.content.Context;
import com.getcapacitor.PluginCall;
import android.util.Log;
import org.forgerock.android.auth.FROptions;
import org.forgerock.android.auth.FROptionsBuilder;
import org.forgerock.android.auth.FRAuth;

public class SDKInitializer {
private static final String TAG = "[SDKInitializer]";

    public static void handle(PluginCall call, Context context) {
        String url = call.getString("url");
        String realm = call.getString("realm");
        String journey = call.getString("journey");
        String oauthClientId = call.getString("oauthClientId");
        String oauthScope = call.getString("oauthScope");

        if (url == null || realm == null || journey == null || oauthClientId == null || oauthScope == null) {
            call.reject("Missing one or more required parameters: url, realm, journey, oauthClientId, oauthScope");
            return;
        }

        try {
            initialize(context, url, realm, journey, oauthClientId, oauthScope);
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "ForgeRock SDK initialization failed", e);
            call.reject("ForgeRock SDK initialization failed", e);
        }
    }

    public static void initialize(Context context, String url, String realm, String journey, String oauthClientId, String oauthScope) {
        Log.d(TAG, "ForgeRock SDK initialize");

        try {
            String bundleId = context.getPackageName() != null ? context.getPackageName() : "com.globalbank.app";

            FROptions options = FROptionsBuilder.build(frOptionsBuilder -> {
                frOptionsBuilder.server(serverBuilder -> {
                    serverBuilder.setUrl(url);
                    serverBuilder.setRealm(realm);
                    serverBuilder.setCookieName("iPlanetDirectoryPro");
                    return null;
                });
                frOptionsBuilder.service(serviceBuilder -> {
                    serviceBuilder.setAuthServiceName(journey);
                    return null;
                });
                frOptionsBuilder.oauth(oauthBuilder -> {
                    oauthBuilder.setOauthClientId(oauthClientId);
                    oauthBuilder.setOauthRedirectUri(bundleId + "://oauth2redirect");
                    oauthBuilder.setOauthScope(oauthScope);
                    return null;
                });
                return null;
            });

            FRAuth.start(context, options);
            Log.d(TAG, "Initialization successful");
        } catch (Exception e) {
            Log.e(TAG, "Error SDK initialization failed", e);
            throw new RuntimeException("Error inicializando ForgeRock SDK", e);
        }
}
}
