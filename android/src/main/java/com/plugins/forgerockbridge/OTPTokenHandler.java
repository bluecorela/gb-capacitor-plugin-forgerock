package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;


import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
import org.forgerock.android.auth.exception.AuthenticatorException;

import com.plugins.forgerockbridge.ErrorHandler;

import java.util.List;

public class OTPTokenHandler {
    private static final String TAG = "ForgeRockBridge";


  public static void startOtpJourney(PluginCall call, Context context, NodeListener<FRSession> listener ) {
        String journey = call.getString("journey");

        if (journey == null) {
          ErrorHandler.reject(call, ErrorHandler.OTPErrorCode.MISSING_JOURNEY);
            return;
        }

        FRSession.authenticate(context, journey, listener);

    }

    public static void validateOTP(PluginCall call, Context context) {
        try {

            FRAClient fraClient = initClient(context);

            List<Account> accounts = fraClient.getAllAccounts();
            JSObject result = new JSObject();

            result.put("empty", accounts.isEmpty());

            call.resolve(result);
        } catch (Exception e) {
          ErrorHandler.reject(call, ErrorHandler.OTPErrorCode.NO_ACCOUNTS_REGISTERED);
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


            JSObject result = new JSObject();
            result.put("otp", otp);
            result.put("expiresIn", expiresIn);

            call.resolve(result);

        } catch (OTPException e) {
          ErrorHandler.reject(call, e.getCode());
        } catch (Exception e) {
            ErrorHandler.reject(call, ErrorHandler.OTPErrorCode.UNKNOWN_ERROR);
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

    private static Account getAccount(FRAClient fraClient) throws OTPException  {
        List<Account> accounts = fraClient.getAllAccounts();
        if (accounts == null || accounts.isEmpty()) {
          throw new OTPException(ErrorHandler.OTPErrorCode.NO_ACCOUNTS_REGISTERED);
        }
        return accounts.get(0);
    }

    private static OathMechanism getOathMechanism(Account account) throws OTPException  {
        for (Mechanism mechanism : account.getMechanisms()) {
            if (mechanism instanceof OathMechanism) {
                return (OathMechanism) mechanism;
            }
        }
      throw new OTPException(ErrorHandler.OTPErrorCode.NO_OTP_REGISTERED);
    }

    public static class OTPException extends Exception {
        private final ErrorHandler.OTPErrorCode code;

        public OTPException(ErrorHandler.OTPErrorCode code) {
            super(code.name());
            this.code = code;
        }

        public ErrorHandler.OTPErrorCode getCode() {
            return code;
        }
    }
}
