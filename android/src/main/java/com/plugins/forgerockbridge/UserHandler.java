package com.plugins.forgerockbridge;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.UserInfo;
import org.json.JSONObject;

public class UserHandler {
private static final String TAG = "ForgeRockBridge ";

   public static void getUserInfo(PluginCall call) {
       Log.e(TAG, "[UserHandler] Getting user information was null or empty");
        if (FRUser.getCurrentUser() != null) {
            FRUser.getCurrentUser().getUserInfo(new FRListener<UserInfo>() {
                @Override
                public void onSuccess(UserInfo result) {
                    try {
                        JSONObject raw = result != null ? result.getRaw() : null;
                        if (raw != null) {
                            JSObject jsUserInfo = JSObject.fromJSONObject(raw);
                            Log.d(TAG, "[UserHandler] Getting user information: " + jsUserInfo);

                            call.resolve(jsUserInfo);
                        } else {
                            Log.e(TAG, "[UserHandler] Getting user information was null or empty");
                            call.reject("userInfo result was null or empty");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[UserHandler] Error parsing user info", e);
                        call.reject("error", e.getMessage(), e);
                    }
                }

                @Override
                public void onException(Exception e) {
                    Log.e(TAG, "[UserHandler] getUserInfo Failed", e);
                    call.reject("error", e.getMessage(), e);
                }
            });

        } else {
            Log.e(TAG, "[UserHandler] Current user is null. Not logged in or SDK not initialized yet");
        }
    }

    public static void logout(PluginCall call) {
        try {
            FRUser currentSession = FRUser.getCurrentUser();

            if (currentSession != null) {
                currentSession.logout();

                    JSObject result = new JSObject();
                    call.resolve(result);

            } else {
                call.reject("No active session to logout.");
            }

        } catch (Exception e) {
            call.reject("Error closing the session: " + e.getMessage());
        }
    }
}
