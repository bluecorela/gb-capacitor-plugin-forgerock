package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
import org.forgerock.android.auth.exception.AuthenticatorException;

import com.plugins.forgerockbridge.nodeListenerCallbacks.OTPDeleteNodeListener;
import com.plugins.forgerockbridge.nodeListenerCallbacks.OTPNodeListener;
import com.plugins.forgerockbridge.state.PluginState;

import java.util.List;

public class OTPTokenHandler {
    private static final String TAG = "ForgeRockBridge";

    public static void startOtpJourney(PluginCall call, Context context, NodeListener<FRSession> listener ) {
        String journey = call.getString("journey");
        
        if (journey == null) {
            call.reject("Missing journey name");
            return;
        }
        
        Log.d(TAG, "journey" + journey);
        
        try {
            FRSession.authenticate(context, journey, listener);
        } catch (Exception e) {
            Log.e(TAG, "[startOtpTreeAuthentication] authenticate error", e);
            call.reject("[startOtpTreeAuthentication] failed: " + e.getMessage(), e);
        }
    }

    public static void validateOTP(PluginCall call, Context context) {
        try {
         
            FRAClient fraClient = initClient(context);

            List<Account> accounts = fraClient.getAllAccounts();
            JSObject result = new JSObject();

            result.put("empty", accounts.isEmpty());

            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "[startOtpTreeAuthentication] authenticate error", e);
            call.reject("authenticate failed: " + e.getMessage(), e);
        }
    }


    public static void generateOTP(PluginCall call, Context context) {
        try {
        
            FRAClient fraClient = initClient(context);

            Account account = getAccount(fraClient);
            OathMechanism mechanism = getOathMechanism(account);
            OathTokenCode token = mechanism.getOathTokenCode();
            String otp = token.getCurrentCode();

            var expiresIn = getRemainingTime(token);

            Log.d(TAG, "Segundos restantes: " + expiresIn);
            Log.d(TAG, "Código OTP actual: " + otp);

            JSObject result = new JSObject();
            result.put("otp", otp);
            result.put("expiresIn", expiresIn);

            call.resolve(result);

        } catch (Exception e) {
            Log.e(TAG, "Error generando OTP", e);
            call.reject(TAG,"Error generando OTP: " + e.getMessage());
        }
    }
    private static long getRemainingTime(OathTokenCode token){
        long until = token.getUntil();
        long now = System.currentTimeMillis();

        return (until - now) / 1000;
    }

    private static FRAClient initClient(Context context) throws AuthenticatorException {
        return new FRAClient.FRAClientBuilder().withContext(context).start();
    }

    private static Account getAccount(FRAClient fraClient) throws OTPException {
        List<Account> accounts = fraClient.getAllAccounts();
        if (accounts == null || accounts.isEmpty()) {
            throw new OTPException("No hay cuentas registradas");
        }
        return accounts.get(0);
    }

    private static OathMechanism getOathMechanism(Account account) throws OTPException {
        for (Mechanism mechanism : account.getMechanisms()) {
            if (mechanism instanceof OathMechanism) {
                return (OathMechanism) mechanism;
            }
        }
        throw new OTPException("Sin OTP registrado");
    }

    private static class OTPException extends Exception {
        public OTPException(String message) {
            super(message);
        }
    }

}
